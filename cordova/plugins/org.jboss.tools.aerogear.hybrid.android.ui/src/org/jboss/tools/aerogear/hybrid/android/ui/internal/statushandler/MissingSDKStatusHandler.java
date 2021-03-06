/*******************************************************************************
 * Copyright (c) 2013,2014 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.aerogear.hybrid.android.ui.internal.statushandler;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.jboss.tools.aerogear.hybrid.android.ui.internal.preferences.AndroidPreferencePage;
import org.jboss.tools.aerogear.hybrid.ui.status.AbstractStatusHandler;

public class MissingSDKStatusHandler extends AbstractStatusHandler {

	@Override
	public void handle(IStatus status) {
		boolean define = MessageDialog.openQuestion(AbstractStatusHandler.getShell(), "Missing Android SDK",
				"Location of the Android SDK must be defined. Define Now?");
		if(define){
			PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(getShell(), AndroidPreferencePage.PAGE_ID, 
					null, null);
			dialog.open();
		}
	}

	@Override
	public void handle(CoreException e) {
		handle(e.getStatus());
	}
}
