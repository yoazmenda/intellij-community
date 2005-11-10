/**
 * created at Sep 17, 2001
 * @author Jeka
 */
package com.intellij.refactoring.changeSignature;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.usageView.*;
import com.intellij.usageView.UsageViewBundle;

class ChangeSignatureViewDescriptor implements UsageViewDescriptor {
  private static final Logger LOG = Logger.getInstance("#com.intellij.refactoring.changeSignature.ChangeSignatureViewDescriptor");

  private PsiMethod myMethod;
  private UsageInfo[] myUsages;
  private final String myProcessedElementsHeader;

  public ChangeSignatureViewDescriptor(PsiMethod method, UsageInfo[] usages) {
    myMethod = method;
    myUsages = usages;
    myProcessedElementsHeader = UsageViewUtil.capitalize(RefactoringBundle.message("0.to.change.signature", UsageViewUtil.getType(method)));
  }

  public PsiElement[] getElements() {
    return new PsiElement[] {myMethod};
  }

  public UsageInfo[] getUsages() {
    return myUsages;
  }

  public String getProcessedElementsHeader() {
    return myProcessedElementsHeader;
  }

  public boolean isSearchInText() {
    return false;
  }

  public boolean toMarkInvalidOrReadonlyUsages() {
    return true;
  }

  public String getCodeReferencesWord(){
    return REFERENCE_WORD;
  }

  public String getCommentReferencesWord(){
    return null;
  }

  public boolean cancelAvailable() {
    return true;
  }

  public String getCodeReferencesText(int usagesCount, int filesCount) {
    return RefactoringBundle.message("references.to.be.changed",
                                     UsageViewBundle.getReferencesString(usagesCount, filesCount));
  }

  public String getCommentReferencesText(int usagesCount, int filesCount) {
    return null;
  }

  public boolean isCancelInCommonGroup() {
    return false;
  }

  public String getHelpID() {
    return "find.refactoringPreview";
  }

  public boolean willUsageBeChanged(UsageInfo usageInfo) {
    return true;
  }

  public boolean canFilterMethods() {
    return true;
  }
}
