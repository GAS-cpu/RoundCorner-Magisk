package com.shizuku.roundcorner;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

/**
 * 屏幕圆角遮罩 View：在屏幕四角绘制黑色圆角遮罩，
 * 把直角屏幕的四个角"涂黑"，视觉上形成圆角屏幕。
 */
public class CornerOverlayView extends View {
    private final Paint paint;
    private float radius; // 圆角半径（像素）

    public CornerOverlayView(Context context, float radiusPx) {
        super(context);
        this.radius = radiusPx;
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (radius <= 0) return;
        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) return;

        // 左上角
        drawTopLeft(canvas, radius);
        // 右上角（水平镜像）
        canvas.save();
        canvas.translate(w, 0);
        canvas.scale(-1f, 1f);
        drawTopLeft(canvas, radius);
        canvas.restore();
        // 左下角（垂直镜像）
        canvas.save();
        canvas.translate(0, h);
        canvas.scale(1f, -1f);
        drawTopLeft(canvas, radius);
        canvas.restore();
        // 右下角（水平+垂直镜像）
        canvas.save();
        canvas.translate(w, h);
        canvas.scale(-1f, -1f);
        drawTopLeft(canvas, radius);
        canvas.restore();
    }

    /** 绘制左上角的圆角遮罩（直角区域减去四分之一圆） */
    private void drawTopLeft(Canvas canvas, float r) {
        Path path = new Path();
        path.moveTo(0f, 0f);
        path.lineTo(0f, r);
        path.arcTo(new RectF(0f, 0f, 2f * r, 2f * r), 180f, 90f, false);
        path.close();
        canvas.drawPath(path, paint);
    }
}
