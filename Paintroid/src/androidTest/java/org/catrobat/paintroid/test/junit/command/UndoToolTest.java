package org.catrobat.paintroid.test.junit.command;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.util.SparseArray;

import org.catrobat.paintroid.PaintroidApplication;
import org.catrobat.paintroid.command.Command;
import org.catrobat.paintroid.command.UndoRedoManager;
import org.catrobat.paintroid.command.implementation.CommandManagerImplementation;
import org.catrobat.paintroid.command.implementation.FillCommand;
import org.catrobat.paintroid.command.implementation.PointCommand;
import org.catrobat.paintroid.datastructures.HistoryBitmap;
import org.catrobat.paintroid.datastructures.LimitedSizeQueue;
import org.catrobat.paintroid.test.utils.PaintroidAsserts;
import org.catrobat.paintroid.test.utils.PrivateAccess;
import org.catrobat.paintroid.tools.Layer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;

import static junit.framework.Assert.assertEquals;

@RunWith(JUnit4.class)
public class UndoToolTest {

    private static int numberLayers;
    private int[] drawCounter;
    private int[] alreadyAdded;
    private static ArrayList<Layer> layers;
    private static ArrayList<Bitmap> bitmaps;
    private SparseArray<LimitedSizeQueue<HistoryBitmap>> historyQueue;
    private SparseArray<LimitedSizeQueue<HistoryBitmap>> savedBitmaps;
    private UndoRedoManager undoRedoManager;
    private int layerHistorySize;
    private static int[] colors = new int[]{
            Color.GREEN,
            Color.BLACK,
            Color.YELLOW,
            Color.BLACK,
            Color.RED,
            Color.CYAN,
            Color.BLUE,
            Color.WHITE,
            Color.WHITE,
            Color.GRAY,
            Color.GRAY,
            Color.DKGRAY,
            Color.MAGENTA,
            Color.LTGRAY
    };

    private enum CommandType {
        PointCommand, FillCommand
    }

    @Before
    public void setUp() throws Exception {
        layerHistorySize = PaintroidApplication.layerHistorySize = 3;
        numberLayers = 3;
        undoRedoManager = UndoRedoManager.getInstance();
        bitmaps = new ArrayList<>(colors.length);
        layers = new ArrayList<>(numberLayers);
        drawCounter = new int[numberLayers];
        alreadyAdded = new int[numberLayers];
        for (int color : colors) {
            bitmaps.add(createBitmap(color));
        }

        for (int i = 0; i < numberLayers; i++) {
            layers.add(new Layer(i, bitmaps.get(0).copy(Bitmap.Config.ARGB_8888, true)));
        }
        historyQueue = (SparseArray<LimitedSizeQueue<HistoryBitmap>>) PrivateAccess.getMemberValue(UndoRedoManager.class, undoRedoManager, "historyQueue");
        savedBitmaps = (SparseArray<LimitedSizeQueue<HistoryBitmap>>) PrivateAccess.getMemberValue(UndoRedoManager.class, undoRedoManager, "savedBitmaps");

    }

    @After
    public void tearDown() throws NoSuchFieldException, IllegalAccessException {
        historyQueue = null;
        savedBitmaps = null;
        undoRedoManager = null;
        PrivateAccess.setMemberValue(UndoRedoManager.class, UndoRedoManager.getInstance(),
                "mInstance", null);
        System.gc();
    }

    @Test
    public void fillUpLayerHistory() {
        addBitmapsToUndoManager(layerHistorySize, CommandType.PointCommand);
    }

    @Test
    public void fillUpBitmapForSpecificCommand() {
        addBitmapsToUndoManager(1, CommandType.FillCommand);
        addBitmapsToUndoManager(layerHistorySize, CommandType.PointCommand);
        checkSizeForAllLayers(1, savedBitmaps);
    }

    @Test
    public void checkHistoryBitmap() {

        addBitmapsToUndoManager(layerHistorySize, CommandType.PointCommand);
        checkSizeForAllLayers(layerHistorySize, historyQueue);
        checkSavedBitmaps(layerHistorySize);
    }

    @Test
    public void checkSavedBitmapForSpecificCommand() {

        addBitmapsToUndoManager(layerHistorySize, CommandType.FillCommand);
        addBitmapsToUndoManager(layerHistorySize * 2, CommandType.PointCommand);

        checkSizeForAllLayers(layerHistorySize, historyQueue);
        checkSizeForAllLayers(layerHistorySize, savedBitmaps);

        checkSavedBitmaps(layerHistorySize);
        checkSizeForAllLayers(0, historyQueue);
        checkSizeForAllLayers(layerHistorySize, savedBitmaps);

        //Simulate Click on Undo
        for (int i = 0; i < numberLayers; i++) {
            drawCounter[i] -= layerHistorySize;
        }

        checkSavedBitmaps(layerHistorySize);
        checkSizeForAllLayers(0, savedBitmaps);

    }

    private static Bitmap createBitmap(int color) {
        Bitmap bitmap = Bitmap.createBitmap(80, 80, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(color);
        return bitmap.copy(Bitmap.Config.ARGB_8888, true);
    }

    private Command createPointCommand() {
        Paint paint = new Paint();
        PointF point = new PointF();
        Command command = new PointCommand(paint, point);
        return command;
    }

    private Command createFillCommand() {
        Paint paint = new Paint();
        Point point = new Point();
        Command command = new FillCommand(point, paint, 12);
        return command;
    }


    private Command getCommand(CommandType commandType) {
        switch (commandType) {
            case PointCommand:
                return createPointCommand();
            case FillCommand:
                return createFillCommand();
        }

        return null;
    }

    private void checkSavedBitmaps(int addedImages) {
        for (Layer layer : layers) {
            for (int i = 0; i < addedImages; i++) {
                int id = layer.getLayerID();
                HistoryBitmap image = undoRedoManager.getImage(layer);
                drawCounter[id]--;
                assertEquals(drawCounter[id], image.getHistoryCount());
                PaintroidAsserts.assertBitmapEquals(bitmaps.get(image.getHistoryCount()), image.getBitmap());
            }
        }
    }

    private void addBitmapUndoManager(Layer layer, CommandType commandType) {
        int id = layer.getLayerID();
        undoRedoManager.saveImage(layer, drawCounter[id], getCommand(commandType));
        alreadyAdded[id]++;
        drawCounter[id]++;
        layer.setImage(bitmaps.get(drawCounter[id]).copy(Bitmap.Config.ARGB_8888, true));
    }

    private void addBitmapsToUndoManager(int amount, CommandType commandType) {
        for (Layer layer : layers) {
            int id = layer.getLayerID();
            for (int i = 0; i < amount; i++) {
                addBitmapUndoManager(layer, commandType);
                int size = historyQueue.get(id).size();
                int expected = Math.min(PaintroidApplication.layerHistorySize, alreadyAdded[id]);
                assertEquals(expected, size);
            }
        }
    }

    private void checkSizeForAllLayers(int expectedSize, SparseArray<LimitedSizeQueue<HistoryBitmap>> queue) {
        for (Layer layer : layers) {
            int size = queue.get(layer.getLayerID()).size();
            assertEquals(expectedSize, size);
        }
    }

}
