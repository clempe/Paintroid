package org.catrobat.paintroid.datastructures;

import android.graphics.Bitmap;

import org.catrobat.paintroid.command.Command;

/**
 * Created by Clemens on 11.05.2017.
 */

public class HistoryBitmap {
    private Command command;
    private int historyCount;
    private Bitmap bitmap;


    public HistoryBitmap(Bitmap bitmap, int historyCount, Command command) {
        this.historyCount = historyCount;
        this.bitmap = bitmap;
        this.command = command;
    }

    public int getHistoryCount() {
        return historyCount;
    }

    public void setHistoryCount(int historyCount) {
        this.historyCount = historyCount;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public Command getCommand() {
        return command;
    }

    public void setCommand(Command command) {
        this.command = command;
    }
}
