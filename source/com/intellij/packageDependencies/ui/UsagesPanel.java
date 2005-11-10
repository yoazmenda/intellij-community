package com.intellij.packageDependencies.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.util.ColorProgressBar;
import com.intellij.openapi.progress.util.ProgressIndicatorBase;
import com.intellij.openapi.project.Project;
import com.intellij.packageDependencies.DependenciesBuilder;
import com.intellij.packageDependencies.FindDependencyUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.usageView.UsageInfo;
import com.intellij.usages.*;
import com.intellij.util.Alarm;
import com.intellij.analysis.AnalysisScopeBundle;

import javax.swing.*;
import java.awt.*;
import java.util.Set;

public class UsagesPanel extends JPanel {
  private static final Logger LOG = Logger.getInstance("#com.intellij.packageDependencies.ui.UsagesPanel");

  private Project myProject;
  private DependenciesBuilder myBuilder;
  private ProgressIndicator myCurrentProgress;
  private JComponent myCurrentComponent;
  private UsageView myCurrentUsageView;
  private Alarm myAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD);

  public UsagesPanel(Project project, DependenciesBuilder builder) {
    super(new BorderLayout());
    myProject = project;
    myBuilder = builder;
    setToInitialPosition();
  }

  public void setToInitialPosition() {
    cancelCurrentFindRequest();
    setToComponent(createLabel(myBuilder.getInitialUsagesPosition()));
  }

  public void findUsages(final Set<PsiFile> searchIn, final Set<PsiFile> searchFor) {
    cancelCurrentFindRequest();

    myAlarm.cancelAllRequests();
    myAlarm.addRequest(new Runnable() {
      public void run() {
        new Thread(new Runnable() {
          public void run() {
            final ProgressIndicator progress = new MyProgressIndicator();
            myCurrentProgress = progress;
            ProgressManager.getInstance().runProcess(new Runnable() {
              public void run() {
                ApplicationManager.getApplication().runReadAction(new Runnable() {
                  public void run() {
                    UsageInfo[] usages = new UsageInfo[0];
                    Set<PsiFile> elementsToSearch = null;

                    try {
                      if (myBuilder.isBackward()){
                        elementsToSearch = searchIn;
                        usages = FindDependencyUtil.findBackwardDependencies(myBuilder, searchFor, searchIn);
                      }
                      else {
                        elementsToSearch = searchFor;
                        usages = FindDependencyUtil.findDependencies(myBuilder, searchIn, searchFor);
                      }
                    }
                    catch (ProcessCanceledException e) {
                    }
                    catch (Exception e) {
                      LOG.error(e);
                    }

                    if (!progress.isCanceled()) {
                      final UsageInfo[] finalUsages = usages;
                      final PsiElement[] _elementsToSearch = elementsToSearch != null? elementsToSearch.toArray(new PsiElement[elementsToSearch.size()]) : PsiElement.EMPTY_ARRAY;
                      ApplicationManager.getApplication().invokeLater(new Runnable() {
                        public void run() {
                          showUsages(new UsageInfoToUsageConverter.TargetElementsDescriptor(_elementsToSearch), finalUsages);
                        }
                      }, ModalityState.stateForComponent(UsagesPanel.this));
                    }
                  }
                });
                myCurrentProgress = null;
              }
            }, progress);
          }
        }).start();
      }
    }, 300);
  }

  private void cancelCurrentFindRequest() {
    if (myCurrentProgress != null) {
      myCurrentProgress.cancel();
    }
  }

  private void showUsages(final UsageInfoToUsageConverter.TargetElementsDescriptor descriptor, final UsageInfo[] usageInfos) {
    try {
      Usage[] usages = UsageInfoToUsageConverter.convert(descriptor, usageInfos);
      UsageViewPresentation presentation = new UsageViewPresentation();
      presentation.setCodeUsagesString(myBuilder.getRootNodeNameInUsageView());
      myCurrentUsageView = myProject.getComponent(UsageViewManager.class).createUsageView(new UsageTarget[0],
                                                                                           usages, presentation, null);
      setToComponent(myCurrentUsageView.getComponent());
    }
    catch (ProcessCanceledException e) {
      setToCanceled();
    }
  }

  private void setToCanceled() {
    setToComponent(createLabel(AnalysisScopeBundle.message("usage.view.canceled")));
  }

  private void setToComponent(final JComponent cmp) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        if (myCurrentComponent != null) {
          if (myCurrentUsageView != null && myCurrentComponent == myCurrentUsageView.getComponent()){
            myCurrentUsageView.dispose();
          }
          remove(myCurrentComponent);
        }
        myCurrentComponent = cmp;
        add(cmp, BorderLayout.CENTER);
        revalidate();
      }
    });
  }

  public void dispose(){
    if (myCurrentUsageView != null){
      myCurrentUsageView.dispose();
    }
  }

  private JComponent createLabel(String text) {
    JLabel label = new JLabel(text);
    label.setHorizontalAlignment(SwingConstants.CENTER);
    return label;
  }

  private class MyProgressIndicator extends ProgressIndicatorBase {
    private MyProgressPanel myProgressPanel;
    private boolean myPaintInQueue;

    public MyProgressIndicator() {
      myProgressPanel = new MyProgressPanel();
    }

    public void start() {
      super.start();
      setToComponent(myProgressPanel.myPanel);
    }

    public void stop() {
      super.stop();
      if (isCanceled()) {
        setToCanceled();
      }
    }

    public void setText(String text) {
      if (!text.equals(getText())) {
        super.setText(text);
        update();
      }
    }

    public void setFraction(double fraction) {
      if (fraction != getFraction()) {
        super.setFraction(fraction);
        update();
      }
    }

    private void update() {
      if (myPaintInQueue) return;
      myPaintInQueue = true;
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          myPaintInQueue = false;
          myProgressPanel.myTextLabel.setText(getText());
          double fraction = getFraction();
          myProgressPanel.myFractionLabel.setText((int)(fraction * 99 + 0.5) + "%");
          myProgressPanel.myFractionProgress.setFraction(fraction);
        }
      });
    }
  }

  private static class MyProgressPanel {
    public JLabel myFractionLabel;
    public ColorProgressBar myFractionProgress;
    public JLabel myTextLabel;
    public JPanel myPanel;
  }
}
