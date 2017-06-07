/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2017 The Catrobat Team
 * (<http://developer.catrobat.org/credits>)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * An additional term exception under section 7 of the GNU Affero
 * General Public License, version 3, is available at
 * http://developer.catrobat.org/license_additional_term
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.catrobat.paintroid.test.espresso.util.base;

import android.app.Activity;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.v4.widget.DrawerLayout;
import android.view.Gravity;

import org.catrobat.paintroid.MainActivity;
import org.catrobat.paintroid.PaintroidApplication;
import org.catrobat.paintroid.test.utils.PrivateAccess;
import org.catrobat.paintroid.test.utils.SystemAnimations;

public class UndoRedoTestRule<T extends Activity> extends ActivityTestRule<T> {
    private SystemAnimations systemAnimations;
    private DrawerLayout drawerLayout;

    public UndoRedoTestRule(Class<T> activityClass) {
        super(activityClass);
    }

    public UndoRedoTestRule(Class<T> activityClass, boolean initialTouchMode) {
        super(activityClass, initialTouchMode);
    }

    public UndoRedoTestRule(Class<T> activityClass, boolean initialTouchMode, boolean launchActivity) {
        super(activityClass, initialTouchMode, launchActivity);
    }

    @Override
    protected void beforeActivityLaunched() {
        super.beforeActivityLaunched();
        PaintroidApplication.numUndoSaves = 2;
        PaintroidApplication.numUndoToolSaves = 3;
    }

    @Override
    protected void afterActivityLaunched() {
        systemAnimations = new SystemAnimations(InstrumentationRegistry.getTargetContext());
        systemAnimations.disableAll();
        lockNavigationDrawer();
        super.afterActivityLaunched();
    }

    @Override
    protected void afterActivityFinished() {
        systemAnimations.enableAll();
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        super.afterActivityFinished();
    }

    void lockNavigationDrawer() {
        try {
            drawerLayout = (DrawerLayout) PrivateAccess.getMemberValue(MainActivity.class, getActivity(), "drawerLayout");
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, Gravity.START);
                }
            });
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}