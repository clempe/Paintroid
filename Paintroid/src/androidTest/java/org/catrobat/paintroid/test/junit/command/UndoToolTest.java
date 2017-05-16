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
import org.catrobat.paintroid.command.implementation.FillCommand;
import org.catrobat.paintroid.command.implementation.PointCommand;
import org.catrobat.paintroid.datastructures.HistoryBitmap;
import org.catrobat.paintroid.datastructures.LimitedSizeQueue;
import org.catrobat.paintroid.test.integration.dialog.ColorDialogIntegrationTest;
import org.catrobat.paintroid.test.utils.PaintroidAsserts;
import org.catrobat.paintroid.test.utils.PrivateAccess;
import org.catrobat.paintroid.tools.Layer;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

@RunWith(JUnit4.class)
public class UndoToolTest {

    private static int numberLayers;
    private ArrayList<Integer> drawCounter;
    private static ArrayList<Layer> layers;
    private static ArrayList<Bitmap> bitmaps;
    private static int[] colors = new int[]{
            Color.GREEN,
            Color.BLACK,
            Color.YELLOW,
            Color.BLACK,
            Color.RED,
            Color.WHITE
    };

    @Before
    public void setUp() throws Exception {
        PaintroidApplication.layerHistorySize = 4;
        numberLayers = 3;
        bitmaps = new ArrayList<>(colors.length);
        layers = new ArrayList<>(numberLayers);
        drawCounter = new ArrayList<>(numberLayers);
        for (int color : colors) {
            bitmaps.add(createBitmap(color));
        }

        for (int i = 0; i < numberLayers; i++) {
            layers.add(new Layer(i, bitmaps.get(0).copy(Bitmap.Config.ARGB_8888, true)));
            drawCounter.add(0);
        }
    }

    @After
    public void tearDown() throws NoSuchFieldException, IllegalAccessException {
        PrivateAccess.setMemberValue(UndoRedoManager.class, UndoRedoManager.getInstance(),
                "mInstance", null);
    }

    @Test
    public void addingQueue() {
        addBitmapUndoManager(layers.get(0), createPointCommand());
        addBitmapUndoManager(layers.get(0), createPointCommand());
        UndoRedoManager.getInstance().getHistoryQueue();
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

    private void addBitmapUndoManager(Layer layer, Command command) {
        Integer drawCounter = this.drawCounter.get(layer.getLayerID());
        UndoRedoManager.getInstance().saveImage(layer, drawCounter,command);
        drawCounter++;
    }

}
