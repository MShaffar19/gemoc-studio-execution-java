/*******************************************************************************
 * Copyright (c) 2016, 2019 Inria and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Inria - initial API and implementation
 *******************************************************************************/
package org.eclipse.gemoc.execution.sequential.javaxdsml.ide.ui.wizards;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.gemoc.commons.eclipse.core.resources.IFileUtils;
import org.eclipse.gemoc.commons.eclipse.core.resources.IProjectUtils;
import org.eclipse.gemoc.commons.eclipse.pde.manifest.ManifestChanger;
import org.eclipse.gemoc.commons.eclipse.pde.wizards.pages.pde.AbstractNewProjectWizardWithTemplates;
import org.eclipse.gemoc.commons.eclipse.pde.wizards.pages.pde.TemplateListSelectionPage;
import org.eclipse.gemoc.commons.eclipse.pde.wizards.pages.pde.WizardElement;
import org.eclipse.gemoc.commons.eclipse.pde.wizards.pages.pde.ui.IProjectContentWizard;
import org.eclipse.gemoc.commons.eclipse.pde.wizards.pages.pde.ui.ProjectTemplateApplicationOperation;
import org.eclipse.gemoc.execution.sequential.javaxdsml.ide.ui.Activator;
import org.eclipse.gemoc.execution.sequential.javaxdsml.ide.ui.builder.GemocSequentialLanguageNature;
import org.eclipse.gemoc.execution.sequential.javaxdsml.ide.ui.wizards.pages.NewGemocLanguageProjectWizardFields;
import org.eclipse.gemoc.execution.sequential.javaxdsml.ide.ui.wizards.pages.NewGemocLanguageProjectWizardPage;
import org.eclipse.gemoc.xdsmlframework.ide.ui.builder.GemocLanguageProjectNature;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.internal.ui.elements.ElementList;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

@SuppressWarnings("restriction")
public class CreateNewGemocSequentialLanguageProject extends AbstractNewProjectWizardWithTemplates implements INewWizard {

	protected NewGemocLanguageProjectWizardFields 		context;
	
	protected NewGemocLanguageProjectWizardPage 		projectPage;
	
	public CreateNewGemocSequentialLanguageProject() {
		super();
		context = new NewGemocLanguageProjectWizardFields();
	}
	
	@Override
	public void addPages() {
		super.addPages();
		
		
		projectPage			 = new NewGemocLanguageProjectWizardPage(this.context);
		//projectPage.setTitle("Project");
		projectPage.setDescription("Create a new Gemoc Sequential Language Project");
		projectPage.updateNameProject("org.company.mysequentiallanguage.xdsml");
		
		addPage(projectPage);			
		addPage(getTemplateListSelectionPage(context));
		
	}
	
	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {	
	}
	
	@Override
	public boolean performFinish() {	
		try {
			IWorkspace workspace = ResourcesPlugin.getWorkspace(); 
			final IProjectDescription description = workspace.newProjectDescription(this.context.projectName);
			if (!this.context.projectLocation.equals(workspace.getRoot().getLocation().toOSString()))
				description.setLocation(new Path(this.context.projectLocation));
			
			final IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(this.context.projectName);
			IWorkspaceRunnable operation = new IWorkspaceRunnable() {
				public void run(IProgressMonitor monitor) throws CoreException {
					project.create(description, monitor);
					project.open(monitor);
					
					configureProject(project, monitor);
					
					
					// launch the template
					
					IProjectContentWizard contentWizard = templateSelectionPage.getSelectedWizard();
					try {
						getContainer().run(false, true, new ProjectTemplateApplicationOperation(context, project, contentWizard));
					} catch (InvocationTargetException e) {
						Activator.error(e.getMessage(), e);
					} catch (InterruptedException e) {
						Activator.error(e.getMessage(), e);
					}
					
					//setClassPath(project, monitor);
					project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
				}
			};
			ResourcesPlugin.getWorkspace().run(operation, null);
			
		} catch (Exception exception) {
			Activator.error(exception.getMessage(), exception);
			return false;
		}
		return true;
	}
	
	public void configureProject(IProject project, IProgressMonitor monitor) {
		try {
			IProjectUtils.addNature(project, "org.eclipse.jdt.core.javanature");
			IProjectUtils.addNature(project, "org.eclipse.xtext.ui.shared.xtextNature");
			IProjectUtils.addNature(project, "org.eclipse.pde.PluginNature");
			
			createEmptyManifestFile(project, monitor);
			updateManifestFile(project, monitor);
			createPlugInFile(project, monitor);
			createBuildProperties(project, monitor);
			
			
			// setclasspath updates the existing description, need to save it first
			setClasspath(project,  monitor);
			
			
			IProjectUtils.addNature(project, GemocLanguageProjectNature.NATURE_ID);
			IProjectUtils.addNature(project, GemocSequentialLanguageNature.NATURE_ID);
			
		} catch (Exception e) {
			Activator.error(e.getMessage(), e);
		}
		
	}
	
    private void createEmptyManifestFile(IProject project, IProgressMonitor monitor) throws Exception {	
	    IFolder metaInf = project.getFolder("META-INF");
	    metaInf.create(false, true, monitor);
	    
	    String path = "META-INF/MANIFEST.MF";
		IContainer currentContainer = project;
		IFile file = currentContainer.getFile(new Path(path));
		String lineSeparator = System.getProperty("line.separator");
		StringBuffer buffer= new StringBuffer();
		buffer.append("Manifest-Version: 1.0" + lineSeparator);
		buffer.append("Bundle-ManifestVersion: 2" + lineSeparator);
		buffer.append("Bundle-Name: " + project.getName() + lineSeparator);
		buffer.append("Bundle-SymbolicName: " + project.getName() + "; singleton:=true" + lineSeparator);
		buffer.append("Automatic-Module-Name: " + project.getName()+ lineSeparator);
		buffer.append("Bundle-Version: 1.0.0" + lineSeparator);
		buffer.append("Bundle-ClassPath: ." + lineSeparator);
	    buffer.append("Bundle-RequiredExecutionEnvironment: JavaSE-1.8"+lineSeparator);
	    //buffer.append("Automatic-Module-Name: " + project.getName());
		IFileUtils.writeInFile(file, buffer.toString(), monitor);    
    }
    private void createBuildProperties(IProject project, IProgressMonitor monitor) throws Exception {	    
	    String path = "build.properties";
		IContainer currentContainer = project;
		IFile file = currentContainer.getFile(new Path(path));
		String contents = GemocProjectFilesTemplates.getBuildProperties();
		IFileUtils.writeInFile(file, contents, monitor);   
    }
	private void updateManifestFile (IProject project, IProgressMonitor monitor) {
		try {
			ManifestChanger manifestChanger = new ManifestChanger(project.getFile("META-INF/MANIFEST.MF"));

			manifestChanger.addPluginDependency("org.eclipse.xtend.lib", "2.21.0", false, true);
			manifestChanger.addPluginDependency("org.eclipse.xtext.xbase.lib", "2.21.0", false, true);
			manifestChanger.addPluginDependency("com.google.guava", "0.0.0", false, true);
			manifestChanger.addPluginDependency("org.eclipse.emf.ecore.xmi", "2.8.0", true, true);
			manifestChanger.addPluginDependency("org.eclipse.emf.ecore", "2.8.0", true, true);
			manifestChanger.addPluginDependency("org.eclipse.emf.common", "2.8.0", true, true);
			manifestChanger.addPluginDependency("fr.inria.diverse.k3.al.annotationprocessor.plugin");
			manifestChanger.addAttributes("Bundle-ActivationPolicy", "lazy");
			manifestChanger.commit();			
		} catch (Exception e) {
			Activator.error(e.getMessage(), e);
		}
	}
	private void createPlugInFile(IProject project,IProgressMonitor monitor) throws Exception {
		String path = "/plugin.xml";
		IContainer currentContainer = project;
		IFile file = currentContainer.getFile(new Path(path));
		
		String contents = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<?eclipse version=\"3.4\"?>\n" + "<plugin>\n" + "</plugin>";
		IFileUtils.writeInFile(file, contents, monitor);
	}
	
	public void setClasspath (IProject project, IProgressMonitor monitor) {
		
		try {
			
			IJavaProject javaProject = (IJavaProject)project.getNature(JavaCore.NATURE_ID);
			
			IFolder sourceFolder = project.getFolder("src");
			try {
				sourceFolder.create(true, true, monitor);
			} catch (Exception ex) {}
			
			ArrayList<IClasspathEntry> newClassPathArrayList = new ArrayList<IClasspathEntry>();
			
			IPackageFragmentRoot root = javaProject.getPackageFragmentRoot(sourceFolder);

			newClassPathArrayList.add( JavaCore.newSourceEntry(root.getPath()));
			newClassPathArrayList.add(JavaCore.newContainerEntry(new Path("org.eclipse.jdt.launching.JRE_CONTAINER")));
			newClassPathArrayList.add(JavaCore.newContainerEntry(new Path("org.eclipse.pde.core.requiredPlugins")));
			//newClassPathArrayList.add(JavaCore.newSourceEntry(javaProject.getPackageFragmentRoot(project.getFolder("src-gen")).getPath()));

			// convert the array to the appropriate table
			IClasspathEntry[] newClassPath = new IClasspathEntry[newClassPathArrayList.size()];
			javaProject.setRawClasspath(newClassPathArrayList.toArray(newClassPath), monitor);
		} catch (Exception e) {
			Activator.error(e.getMessage(), e);	
		}
		
	}
	/**
	 * Look for extension point="org.eclipse.gemoc.commons.eclipse.pde.projectContent"
	 * and filter wizards
	 */
	@Override
	public ElementList getAvailableCodegenWizards() {
		ElementList superRes = super.getAvailableCodegenWizards();
		ElementList newRes = new ElementList("CodegenWizards"); //$NON-NLS-1$
		
		for (Object element : superRes.getChildren()) {
			if(element instanceof WizardElement){
				WizardElement wizardElem = (WizardElement) element;
				String id = wizardElem.getID();
				if(id.equals("org.eclipse.gemoc.execution.sequential.javaxdsml.ide.ui.templates.projectContent.SequentialSingleLanguage")) {
					newRes.add(wizardElem);
				}
			}
		}
		
		return newRes;
	}
	
	@Override
	public void createPageControls(Composite pageContainer) {
		super.createPageControls(pageContainer);
		TemplateListSelectionPage templatesPage = getTemplateListSelectionPage(context);
		templatesPage.setUseTemplate(true);
		// select this template as default
		templatesPage.selectTemplate("org.eclipse.gemoc.execution.sequential.javaxdsml.ide.ui.templates.projectContent.SequentialSingleLanguage");
	}
	
	
	
	@Override
	public String getTargetPluginId() {		
		return Activator.getDefault().getBundle().getSymbolicName();
	}
}
