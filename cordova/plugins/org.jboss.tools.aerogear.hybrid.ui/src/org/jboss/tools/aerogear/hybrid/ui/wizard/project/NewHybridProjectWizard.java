/*******************************************************************************
 * Copyright (c) 2013, 2014 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.aerogear.hybrid.ui.wizard.project;

import static org.jboss.tools.aerogear.hybrid.ui.plugins.internal.CordovaPluginSelectionPage.PLUGIN_SOURCE_DIRECTORY;
import static org.jboss.tools.aerogear.hybrid.ui.plugins.internal.CordovaPluginSelectionPage.PLUGIN_SOURCE_GIT;
import static org.jboss.tools.aerogear.hybrid.ui.plugins.internal.CordovaPluginSelectionPage.PLUGIN_SOURCE_REGISTRY;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;
import org.jboss.tools.aerogear.hybrid.core.HybridProject;
import org.jboss.tools.aerogear.hybrid.core.engine.HybridMobileEngine;
import org.jboss.tools.aerogear.hybrid.core.plugin.CordovaPluginManager;
import org.jboss.tools.aerogear.hybrid.core.plugin.FileOverwriteCallback;
import org.jboss.tools.aerogear.hybrid.core.plugin.registry.CordovaPluginRegistryManager;
import org.jboss.tools.aerogear.hybrid.core.plugin.registry.CordovaRegistryPluginVersion;
import org.jboss.tools.aerogear.hybrid.ui.HybridUI;
import org.jboss.tools.aerogear.hybrid.ui.internal.status.StatusManager;
import org.jboss.tools.aerogear.hybrid.ui.plugins.internal.CordovaPluginSelectionPage;
import org.jboss.tools.aerogear.hybrid.ui.plugins.internal.ICordovaPluginWizard;
import org.jboss.tools.aerogear.hybrid.ui.plugins.internal.RegistryConfirmPage;

public class NewHybridProjectWizard extends Wizard implements INewWizard,ICordovaPluginWizard {
	private static final String IMAGE_WIZBAN = "/icons/wizban/newcordovaprj_wiz.png";

	private IWizardPage pageOne;
	private EngineConfigurationPage pageTwo;
	private CordovaPluginSelectionPage pageThree;
	private RegistryConfirmPage pageFour;

	public NewHybridProjectWizard() {
		setWindowTitle("Hybrid Mobile (Cordova) Application Project");
		setDefaultPageImageDescriptor(HybridUI.getImageDescriptor(HybridUI.PLUGIN_ID, IMAGE_WIZBAN));
		
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
	}

	@Override
	public boolean performFinish() {
		IRunnableWithProgress runnable = new IRunnableWithProgress() {
			
			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException,
					InterruptedException {
				HybridProjectCreator creator = new HybridProjectCreator();
				WizardNewHybridProjectCreationPage page = (WizardNewHybridProjectCreationPage)pageOne;
				EngineConfigurationPage enginePage = (EngineConfigurationPage)pageTwo;
				try {
					URI location = null;
					if( !page.useDefaults() ){
						location = page.getLocationURI();
					}
					String appName = page.getApplicationName();
					String appID = page.getApplicationID();
					HybridMobileEngine engine = enginePage.getSelectedEngine();
					IProject project = creator.createBasicTemplatedProject(page.getProjectName(), location ,appName, appID, engine, monitor);
					installSelectedPlugins(project, monitor);
					openAndSelectConfigFile(project);
					
					} catch (CoreException e) {
						throw new InvocationTargetException(e);
				}
				
			}
		};
		
		try {
			getContainer().run(false, true, runnable);
		} catch (InvocationTargetException e) {
			if (e.getTargetException() != null) {
				if(e.getTargetException() instanceof CoreException ){
					StatusManager.handle((CoreException) e.getTargetException());
				}else{
					ErrorDialog.openError(getShell(), "Error creating project",null, 
							new Status(IStatus.ERROR, HybridUI.PLUGIN_ID, "Project create error", e.getTargetException() ));
				}
			}
		} catch (InterruptedException e) {
			throw new OperationCanceledException();
		}
		return true;
	}
	
	private void installSelectedPlugins(IProject project, IProgressMonitor monitor) throws CoreException{
		Assert.isNotNull(project);
		HybridProject hybridProject = HybridProject.getHybridProject(project);
		FileOverwriteCallback cb = new FileOverwriteCallback() {
			@Override
			public boolean isOverwiteAllowed(String[] files) {
				return true;
			}
		};
		CordovaPluginManager pm = new CordovaPluginManager(hybridProject);
		switch (pageThree.getPluginSourceType()){
		case PLUGIN_SOURCE_DIRECTORY:
			File directory = new File(pageThree.getSelectedDirectory());
			pm.installPlugin(directory,cb, monitor);
			break;
		case PLUGIN_SOURCE_GIT:
			URI uri = URI.create(pageThree.getSpecifiedGitURL());
			pm.installPlugin(uri,null,null,cb,monitor );
			break;
		case PLUGIN_SOURCE_REGISTRY:
			List<CordovaRegistryPluginVersion> plugins = pageFour.getSelectedPluginVersions();
			CordovaPluginRegistryManager regMgr = new CordovaPluginRegistryManager(CordovaPluginRegistryManager.DEFAULT_REGISTRY_URL);
			for (CordovaRegistryPluginVersion cordovaRegistryPluginVersion : plugins) {
				pm.installPlugin(regMgr.getInstallationDirectory(cordovaRegistryPluginVersion,monitor),cb,monitor);
			}
			break;
		default:
			Assert.isTrue(false, "No valid plugin source can be determined");
			break;
		}

	}
	
	private void openAndSelectConfigFile(IProject project){
		IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		HybridProject hp = HybridProject.getHybridProject(project);
		IFile file = hp.getConfigFile();
		
		BasicNewResourceWizard.selectAndReveal(file, PlatformUI.getWorkbench().getActiveWorkbenchWindow());
		try {
			IDE.openEditor(activePage, file);
		} catch (PartInitException e) {
			HybridUI.log(IStatus.ERROR, "Error opening the config.xml", e);
		}
	}
	
	@Override
	public void addPages() {
		super.addPages();
		pageOne = new WizardNewHybridProjectCreationPage(getWindowTitle());
		addPage( pageOne );
		pageTwo = new EngineConfigurationPage("Configure Engine");
		addPage( pageTwo);
		pageThree = new CordovaPluginSelectionPage(true);
		addPage(pageThree);
		pageFour = new RegistryConfirmPage();
		addPage(pageFour);
	}

	@Override
	public WizardPage getRegistryConfirmPage() {
		return pageFour;
	}

	@Override
	public boolean isPluginSelectionOptional() {
		return true;
	}

}
