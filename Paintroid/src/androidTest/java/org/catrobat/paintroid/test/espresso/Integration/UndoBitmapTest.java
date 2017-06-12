/**
 * Paintroid: An image manipulation application for Android.
 * Copyright (C) 2010-2015 The Catrobat Team
 * (<http://developer.catrobat.org/credits>)
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * <p>
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.catrobat.paintroid.test.espresso.Integration;

import android.graphics.Bitmap;
import android.graphics.PointF;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.action.Swipe;
import android.util.SparseArray;

import junit.framework.Assert;

import org.catrobat.paintroid.MainActivity;
import org.catrobat.paintroid.PaintroidApplication;
import org.catrobat.paintroid.R;
import org.catrobat.paintroid.command.LayerBitmapCommand;
import org.catrobat.paintroid.command.UndoRedoManager;
import org.catrobat.paintroid.command.implementation.CommandManagerImplementation;
import org.catrobat.paintroid.command.implementation.FillCommand;
import org.catrobat.paintroid.datastructures.HistoryBitmap;
import org.catrobat.paintroid.datastructures.LimitedSizeQueue;
import org.catrobat.paintroid.dialog.IndeterminateProgressDialog;
import org.catrobat.paintroid.listener.LayerListener;
import org.catrobat.paintroid.test.espresso.util.ActivityHelper;
import org.catrobat.paintroid.test.espresso.util.DialogHiddenIdlingResource;
import org.catrobat.paintroid.test.espresso.util.base.UndoTestRule;
import org.catrobat.paintroid.test.utils.PrivateAccess;
import org.catrobat.paintroid.tools.Layer;
import org.catrobat.paintroid.tools.ToolType;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedList;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.isRoot;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static junit.framework.TestCase.assertEquals;
import static org.catrobat.paintroid.PaintroidApplication.numUndoSaves;
import static org.catrobat.paintroid.PaintroidApplication.numUndoToolSaves;
import static org.catrobat.paintroid.PaintroidApplication.perspective;
import static org.catrobat.paintroid.test.espresso.util.EspressoUtils.addLayers;
import static org.catrobat.paintroid.test.espresso.util.EspressoUtils.convertFromCanvasToScreen;
import static org.catrobat.paintroid.test.espresso.util.EspressoUtils.drawHorizontalLine;
import static org.catrobat.paintroid.test.espresso.util.EspressoUtils.getCurrentLayer;
import static org.catrobat.paintroid.test.espresso.util.EspressoUtils.getLayer;
import static org.catrobat.paintroid.test.espresso.util.EspressoUtils.getWorkingBitmap;
import static org.catrobat.paintroid.test.espresso.util.EspressoUtils.resetColorPicker;
import static org.catrobat.paintroid.test.espresso.util.EspressoUtils.selectLayerAtPosition;
import static org.catrobat.paintroid.test.espresso.util.EspressoUtils.selectTool;
import static org.catrobat.paintroid.test.espresso.util.EspressoUtils.waitMillis;
import static org.catrobat.paintroid.test.espresso.util.UiInteractions.swipeMode;
import static org.catrobat.paintroid.test.espresso.util.UiInteractions.touchAt;
import static org.catrobat.paintroid.test.utils.PaintroidAsserts.assertBitmapEquals;
import static org.hamcrest.Matchers.anything;


@RunWith(JUnit4.class)
public class UndoBitmapTest {

    @Rule
    public UndoTestRule<MainActivity> activityTestRule = new UndoTestRule(MainActivity.class);

    private ActivityHelper activityHelper;

    private int displayWidth;
    private int displayHeight;
    private int lineLength;
    private SparseArray<LinkedList<Bitmap>> bitmapHistory;
    private SparseArray<LimitedSizeQueue<HistoryBitmap>> historyQueue;
    private SparseArray<LimitedSizeQueue<HistoryBitmap>> savedBitmaps;
    private int drawCounter[];

    private PointF pointOnScreenMiddle;
    private ArrayList<LayerBitmapCommand> mDrawBitmapCommandsAtLayer;
    private DialogHiddenIdlingResource dialogWait;
    private Bitmap workingBitmap;

    @Before
    public void setUp() throws NoSuchFieldException, IllegalAccessException {
        activityHelper = new ActivityHelper(activityTestRule.getActivity());
        PaintroidApplication.drawingSurface.destroyDrawingCache();
        dialogWait = new DialogHiddenIdlingResource(IndeterminateProgressDialog.getInstance(), activityTestRule.getActivity().getSupportFragmentManager());
        Espresso.registerIdlingResources(dialogWait);

        displayWidth = activityHelper.getDisplayWidth();
        displayHeight = activityHelper.getDisplayHeight();
        lineLength = displayWidth + 200;
        bitmapHistory = new SparseArray<>();

        pointOnScreenMiddle = new PointF(displayWidth / 2, displayHeight / 2);
        swipeMode = Swipe.FAST;
        resetColorPicker();

        UndoRedoManager undoRedoManager = UndoRedoManager.getInstance();
        historyQueue = (SparseArray<LimitedSizeQueue<HistoryBitmap>>) PrivateAccess.getMemberValue(UndoRedoManager.class, undoRedoManager, "undoArray");
        savedBitmaps = (SparseArray<LimitedSizeQueue<HistoryBitmap>>) PrivateAccess.getMemberValue(UndoRedoManager.class, undoRedoManager, "savedBitmaps");
        mDrawBitmapCommandsAtLayer = (ArrayList<LayerBitmapCommand>) PrivateAccess.getMemberValue(CommandManagerImplementation.class, PaintroidApplication.commandManager, "mDrawBitmapCommandsAtLayer");
        PrivateAccess.setMemberValue(UndoRedoManager.class, undoRedoManager, "nThCommand", 100);
        PrivateAccess.setMemberValue(UndoRedoManager.class, undoRedoManager, "commandToStore", new Class<?>[]{FillCommand.class});
        workingBitmap = getWorkingBitmap();
        PaintroidApplication.numUndoToolSaves = 2;
        numUndoSaves = 2;
        PaintroidApplication.perspective.resetScaleAndTranslation();
    }

    @After
    public void tearDown() {
        Espresso.unregisterIdlingResources(dialogWait);
        displayWidth = 0;
        lineLength = 0;
        displayHeight = 0;
        displayWidth = 0;
        displayHeight = 0;
        drawCounter = null;
        activityHelper = null;
        pointOnScreenMiddle = null;
        bitmapHistory.clear();
        bitmapHistory = null;
        mDrawBitmapCommandsAtLayer = null;



        if (workingBitmap != null && !workingBitmap.isRecycled()) {
            workingBitmap.recycle();
        }

        workingBitmap = null;
        PaintroidApplication.perspective.resetScaleAndTranslation();
    }

    @Test
    public void checkDrawCounterSingleLayer() throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        selectTool(ToolType.BRUSH);
        Layer layer = getLayer(0);
        int numDraw = 5;
        int xSpacing = displayWidth / (3 * numDraw + 1);
        drawCounter = new int[]{0};
        initHistoryBitmap(1);

        assertEquals(drawCounter[0], getLayerBitmapCommand(layer).getDrawingCount());

        for (int i = 0; i < numDraw; i++) {
            pointOnScreenMiddle.x += xSpacing;
            drawDotAndCheck(pointOnScreenMiddle);
        }

        for (int i = 0; i < numDraw; i++) {
            performUndoAndCheck();
        }
    }

    @Test
    public void checkDrawCounterMultipleLayer() throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        selectTool(ToolType.BRUSH);
        int numDraw = 5;
        int numLayer = 3;
        int xSpacing = displayWidth / (3 * numDraw + 1);
        drawCounter = new int[numLayer];

        addLayers(numLayer - 1);
        initHistoryBitmap(numLayer);

        for (int l = 0; l < numLayer; l++) {
            PointF startPoint = new PointF(displayWidth / 2, displayHeight / 2);
            selectLayerAtPosition(numLayer - 1 - l);
            assertEquals(l, getCurrentLayer().getLayerID());

            for (int i = 0; i < numDraw; i++) {
                startPoint.x += xSpacing;
                drawDotAndCheck(startPoint);
            }

            for (int i = 0; i < numDraw; i++) {
                performUndoAndCheck();
            }
        }
    }

    @Test
    public void drawLinesAndUndo() throws NoSuchFieldException, IllegalAccessException {
        int num_draws = 3;
        int xStart = 0;
        int yStart = (int) (displayHeight * 0.2);
        int ySpacing = displayWidth / (num_draws + 1);
        Layer layer = getLayer(0);
        int id = layer.getLayerID();
        initHistoryBitmap(1);
        drawCounter = new int[]{0};


        for (int i = 0; i < num_draws; i++) {
            int y = yStart + drawCounter[id] * ySpacing;
            drawHorizontalLine(xStart, lineLength, y);
            bitmapHistory.get(id).add(layer.getImageCopy());
            drawCounter[id]++;
        }

        LimitedSizeQueue<HistoryBitmap> layer0Queue = historyQueue.get(id);
        int expected = Math.min(drawCounter[id], numUndoSaves);
        assertEquals(expected, layer0Queue.size());
        assertBitmapEquals(bitmapHistory.get(id).get(drawCounter[id] - 1), layer0Queue.getYoungest().getBitmap());

        for (int i = 0; i < num_draws; i++) {
            onView(withId(R.id.btn_top_undo)).perform(click());
            drawCounter[id]--;
            assertBitmapEquals(bitmapHistory.get(id).get(drawCounter[id]), layer.getImage());
        }
    }

    @Test
    public void drawLinesFillAndUndo() throws NoSuchFieldException, IllegalAccessException, InterruptedException {
        int num_draws = 3;
        int xStart = 0;
        int yStart = (int) (displayHeight * 0.2);
        int ySpacing = displayHeight / (num_draws + 1);
        int y;
        drawCounter = new int[]{0};
        Layer layer = getCurrentLayer();
        int id = layer.getLayerID();
        initHistoryBitmap(1);

        for (int i = 0; i < num_draws; i++) {
            y = yStart + i * ySpacing;
            drawHorizontalLineAndFillAbove(xStart, lineLength, y, ySpacing);
        }

        LimitedSizeQueue<HistoryBitmap> layer0Queue = historyQueue.get(id);
        assertEquals(layer0Queue.getYoungest().getHistoryCount(), drawCounter[id] - 1);
        assertEquals(Math.min(numUndoSaves, num_draws), layer0Queue.size());
        assertEquals(Math.min(numUndoToolSaves, num_draws), savedBitmaps.get(id).size());


        while (drawCounter[id] != 0) {
            onView(withId(R.id.btn_top_undo)).perform(click());
            drawCounter[id]--;
            assertBitmapEquals(bitmapHistory.get(id).get(drawCounter[id]), layer.getImage());
        }
    }



    @Test
    public void undoMultipleLayer() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        int numLayer = 3;
        int numDraw = 5;
        int totalNumberDraws = 0;
        int xSpacing = displayWidth / 2 / (numDraw + 1);
        int[] layerDrawingOrder = new int[] {0, 2 , 1};
        int[] layerDrawingCount = new int[] {3, 2 , 4};

        int ySpacing = (int) (displayHeight * 0.1);
        drawCounter = new int[numLayer];

        addLayers(numLayer - 1);
        initHistoryBitmap(numLayer);

        Assert.assertEquals("Wrong Number of Layers", numLayer, LayerListener.getInstance().getAdapter().getCount());


        for (int layerId : layerDrawingOrder) {
            PointF startPoint = new PointF(displayWidth / 2, displayHeight / 2);
            int layerDrawerPosition = numLayer - 1 - layerId;
            selectLayerAtPosition(layerDrawerPosition);
            assertEquals("Wrong Layer selected", layerId, getCurrentLayer().getLayerID());

            startPoint.y += layerId * ySpacing;
            for (int i = 0; i < layerDrawingCount[layerId]; i++) {
                drawDotAndCheck(startPoint);
                startPoint.x += xSpacing;
                totalNumberDraws++;
            }
        }

        selectLayerAtPosition(2);
        assertEquals("Wrong Layer selected", layerDrawingOrder[0], getCurrentLayer().getLayerID());


        for (int j = layerDrawingOrder.length -1; j > 0; j--) {
            int layerId = layerDrawingOrder[j];
            for (int i = 0; i < layerDrawingCount[layerId]; i++) {
                performUndoAndCheck(getLayer(layerId));
            }
        }
    }

    @Test
    public void checkSaveNthImage() throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        int nth = 3;
        PrivateAccess.setMemberValue(UndoRedoManager.class, UndoRedoManager.getInstance(), "nThCommand", nth);
        drawCounter = new int[]{0};
        initHistoryBitmap(1);

        for (int i = 0; i < nth*numUndoSaves+1 + 1; i++) {
            drawDotAndCheck(pointOnScreenMiddle);
        }

        int size = savedBitmaps.get(getCurrentLayer().getLayerID()).size();
        assertEquals(1, size);

    }

    private LayerBitmapCommand getLayerBitmapCommand(Layer layer) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        for (LayerBitmapCommand layerBitmapCommand : mDrawBitmapCommandsAtLayer) {
            if (layerBitmapCommand.getLayer().getLayerID() == layer.getLayerID()) {
                return layerBitmapCommand;
            }
        }
        throw new IndexOutOfBoundsException();
    }

    private void drawHorizontalLineAndFillAbove(int xStart, int xEnd, int y, int ySpacing) throws NoSuchFieldException, IllegalAccessException, InterruptedException {
        int id = getCurrentLayer().getLayerID();
        drawHorizontalLine(xStart, xEnd, y);
        drawCounter[id]++;
        bitmapHistory.get(id).add(getCurrentLayer().getImageCopy());
        selectTool(ToolType.FILL);
        PointF pointF = convertFromCanvasToScreen(new PointF(pointOnScreenMiddle.x, y - ySpacing / 2), perspective);
        onView(isRoot()).perform(touchAt(pointF));
        waitMillis(300);
        bitmapHistory.get(id).add(getCurrentLayer().getImageCopy());
        drawCounter[id]++;
    }


    private void initHistoryBitmap(int numLayers) {
        for (int i = 0; i < numLayers; i++) {
            bitmapHistory.put(i, new LinkedList<Bitmap>());
            bitmapHistory.get(i).add(getLayer(i).getImageCopy());
        }
    }

    private void drawDotAndCheck(PointF pointOnScreenMiddle) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Layer currentLayer = getCurrentLayer();
        int id = currentLayer.getLayerID();
        onView(isRoot()).perform(touchAt(pointOnScreenMiddle));
        drawCounter[id]++;
        bitmapHistory.get(id).add(currentLayer.getImageCopy());
        assertEquals(drawCounter[id], getLayerBitmapCommand(currentLayer).getDrawingCount());
    }

    private void performUndoAndCheck() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        performUndoAndCheck(getCurrentLayer());
    }

    private void performUndoAndCheck(Layer layer) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        int id = layer.getLayerID();
        onView(withId(R.id.btn_top_undo)).perform(click());
        drawCounter[id]--;
        assertEquals(drawCounter[id], getLayerBitmapCommand(layer).getDrawingCount());
        assertBitmapEquals(bitmapHistory.get(id).get(drawCounter[id]), layer.getImageCopy());
    }
}
