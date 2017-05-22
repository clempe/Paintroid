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

package org.catrobat.paintroid.command;

import android.os.Debug;
import android.util.Log;
import android.util.SparseArray;

import org.catrobat.paintroid.PaintroidApplication;
import org.catrobat.paintroid.command.implementation.CommandManagerImplementation;
import org.catrobat.paintroid.command.implementation.FillCommand;
import org.catrobat.paintroid.command.implementation.LayerCommand;
import org.catrobat.paintroid.command.implementation.StampCommand;
import org.catrobat.paintroid.datastructures.HistoryBitmap;
import org.catrobat.paintroid.datastructures.LimitedSizeQueue;
import org.catrobat.paintroid.listener.LayerListener;
import org.catrobat.paintroid.tools.Layer;
import org.catrobat.paintroid.ui.TopBar;

import static org.catrobat.paintroid.PaintroidApplication.TAG;

public final class UndoRedoManager {

    private static UndoRedoManager mInstance;
    private TopBar mTopBar;
    private boolean enableUndo = false;
    private boolean enableRedo = false;
    private SparseArray<LimitedSizeQueue<HistoryBitmap>> historyQueue = new SparseArray<>();
    private SparseArray<LimitedSizeQueue<HistoryBitmap>> savedBitmaps = new SparseArray<>();

    private UndoRedoManager() {

    }

    public static UndoRedoManager getInstance() {
        if (mInstance == null) {
            mInstance = new UndoRedoManager();
        }
        return mInstance;
    }

    public void setStatusbar(TopBar topBar) {
        mTopBar = topBar;

    }

    public void update() {
        Layer currentLayer = LayerListener.getInstance().getCurrentLayer();
        LayerCommand layerCommand = new LayerCommand(currentLayer);
        LayerBitmapCommand layerBitmapCommand = PaintroidApplication.commandManager
                .getLayerBitmapCommand(layerCommand);


        handleUndo(layerBitmapCommand);
        handleRedo(layerBitmapCommand);
    }

    private void handleUndo(LayerBitmapCommand layerBitmapCommand) {
        if (!CommandManagerImplementation.getInstance().mLayersCommandHistory.isEmpty())
            PaintroidApplication.commandManager.enableUndo(true);
        else
            PaintroidApplication.commandManager.enableUndo(false);

        if (!CommandManagerImplementation.getInstance().mLayersCommandUndo.isEmpty())
            PaintroidApplication.commandManager.enableRedo(true);
        else
            PaintroidApplication.commandManager.enableRedo(false);
    }

    private void handleRedo(LayerBitmapCommand layerBitmapCommand) {
        if (!CommandManagerImplementation.getInstance().mLayersCommandHistory.isEmpty())
            PaintroidApplication.commandManager.enableUndo(true);
        else
            PaintroidApplication.commandManager.enableUndo(false);

        if (!CommandManagerImplementation.getInstance().mLayersCommandUndo.isEmpty())
            PaintroidApplication.commandManager.enableRedo(true);
        else
            PaintroidApplication.commandManager.enableRedo(false);
    }
//
//	private void handleUndo(LayerBitmapCommand layerBitmapCommand) {
//		if(layerBitmapCommand.getLayerCommands().size() != 0)
//			PaintroidApplication.commandManager.enableUndo(true);
//		else
//			PaintroidApplication.commandManager.enableUndo(false);
//		if(layerBitmapCommand.getLayerUndoCommands().size() != 0)
//			PaintroidApplication.commandManager.enableRedo(true);
//		else
//			PaintroidApplication.commandManager.enableRedo(false);
//	}
//
//	private void handleRedo(LayerBitmapCommand layerBitmapCommand) {
//		if(layerBitmapCommand.getLayerCommands().size() != 0)
//			PaintroidApplication.commandManager.enableUndo(true);
//		else
//			PaintroidApplication.commandManager.enableUndo(false);
//		if(layerBitmapCommand.getLayerUndoCommands().size() != 0)
//			PaintroidApplication.commandManager.enableRedo(true);
//		else
//			PaintroidApplication.commandManager.enableRedo(false);
//	}
//
//	public void update(StatusMode status) {
//		switch (status) {
//		case ENABLE_UNDO:
//			mTopBar.toggleUndo(R.drawable.icon_menu_undo);
//			mTopBar.enableUndo();
//
//			break;
//		case DISABLE_UNDO:
//			mTopBar.toggleUndo(R.drawable.icon_menu_undo_disabled);
//			mTopBar.disableUndo();
//			break;
//		case ENABLE_REDO:
//			mTopBar.toggleRedo(R.drawable.icon_menu_redo);
//			mTopBar.enableRedo();
//			break;
//		case DISABLE_REDO:
//			mTopBar.toggleRedo(R.drawable.icon_menu_redo_disabled);
//			mTopBar.disableRedo();
//			break;
//
//		default:
//			break;
//		}
//	}

    private LimitedSizeQueue<HistoryBitmap> createBitmapQueue() {
        return new LimitedSizeQueue<>(PaintroidApplication.layerHistorySize);
    }

    public void saveImage(Layer layer, int drawingState, Command command) {
        int id = layer.getLayerID();

        LimitedSizeQueue<HistoryBitmap> queue = getQueue(historyQueue, id);
        HistoryBitmap historyBitmap = new HistoryBitmap(layer.getImageCopy(), drawingState, command);

        HistoryBitmap removed = queue.add(historyBitmap);
        Log.e(TAG, queue.toString());

        Log.d(TAG, "Added Bitmap to layer " + id + " Size: "
                + queue.size() + "/" + queue.maxSize());


        if (removed != null &&
                (removed.getCommand() instanceof StampCommand ||
                 removed.getCommand() instanceof FillCommand)) {
            Log.e(TAG, "Keep Bitmap Stored  because of Command Type Layer: " + id);
            queue = getQueue(savedBitmaps, id);
            queue.add(removed);
        }

        Log.e(TAG, historyQueue.get(0).toString());
    }

    private LimitedSizeQueue<HistoryBitmap> getQueue(SparseArray<LimitedSizeQueue<HistoryBitmap>> queue, int id) {
        LimitedSizeQueue<HistoryBitmap> ret = queue.get(id);
        if(ret == null){
            ret = createBitmapQueue();
            queue.put(id, ret);
        }
        Log.e(TAG, queue.toString());
        return ret;
    }

    public HistoryBitmap getImage(Layer layer) {
        int id = layer.getLayerID();
        LimitedSizeQueue<HistoryBitmap> queue = historyQueue.get(id);
        if (queue != null && !queue.isEmpty()) {
            Log.d(TAG, "Get Bitmap from History queue, Layer: " + id);
            return queue.pop();
        }

        queue = savedBitmaps.get(id);

        if (queue != null && !queue.isEmpty()) {
            Log.d(TAG, "Get Bitmap from Saved Command Stack, Layer: " + id);
            return queue.pop();
        }

        Log.d(TAG, "No Image for Layer " + id + "stored");

        return null;
    }

}
