package com.example.broadkast;

import java.nio.ByteBuffer;

import android.content.Context;
import android.graphics.Canvas;
import android.util.Log;
import android.view.View;

public class StreamView extends View {

	 private int pixels[];
	 final int WIDTH = 736;
	 final int HEIGHT = 1280;
	 private String TAG;
	 public StreamView(Context context) {
		 super(context);
		 TAG = getClass().getName();
		 // TODO Auto-generated constructor stub
		 pixels = new int [WIDTH*HEIGHT];
		 for (int i = 0 ; i < 736*1280; i ++){
			 pixels[i] = 0x00; 
		 }
	 }
	
	
	@Override
	protected void onDraw(Canvas canvas) {
		// TODO Auto-generated method stub
		Log.i(TAG, "onDraw called");
	 
		canvas.drawBitmap(pixels, 0, WIDTH, 0, 0, WIDTH, HEIGHT, false, null);
		invalidate();
	}
	
	public void updatePixels(int screen[])
	{
		pixels = screen;
	}
	
	public void updatePixels(byte screen[])
	{
		Log.i(TAG, "updatePixels called");
		ByteBuffer bb = ByteBuffer.wrap(screen);
		bb.asIntBuffer().get(pixels);
		Log.i(TAG, "updatePixels exiting");
	}
}

