package com.krmylmz.characterrecognizer.drawing;

import android.graphics.Path;

public class DrawPath {

    private int color;
    private static int strokeWidth = 60;
    private Path path;

    public DrawPath(int color, Path path){
        this.color = color;
        this.path = path;
    }

    protected void setColor(int color){
        this.color = color;
    }

    public int getColor(){
        return this.color;
    }

    public Path getPath(){
        return this.path;
    }

    protected void setPath(Path path){
        this.path = path;
    }

    public static int getStrokeWidth() {
        return strokeWidth;
    }
}
