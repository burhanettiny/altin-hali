package com.burhanettin.altinhali

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.view.View
import kotlin.math.sin
import kotlin.random.Random

/** Menülerin arkasında yavaşça yükselen köpük baloncukları ve altınlar. */
class MenuArka(ctx: Context) : View(ctx) {
    private class Kabarcik(var x: Float, var y: Float, val r: Float, val hiz: Float, val faz: Float, val altin: Boolean)

    private val d = resources.displayMetrics.density
    private val rnd = Random(3)
    private val liste = ArrayList<Kabarcik>()
    private val boya = Paint(Paint.ANTI_ALIAS_FLAG)
    private var gradyan: LinearGradient? = null
    private var son = 0L
    private var zaman = 0f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        gradyan = LinearGradient(0f, 0f, 0f, h.toFloat(), 0xFF3E1F47.toInt(), 0xFF1B2A35.toInt(), Shader.TileMode.CLAMP)
        liste.clear()
        repeat(24) { liste.add(yeni(w, h, rnd.nextFloat() * h)) }
    }

    private fun yeni(w: Int, h: Int, y: Float) = Kabarcik(
        rnd.nextFloat() * w, y, (4f + rnd.nextFloat() * 16f) * d, (14f + rnd.nextFloat() * 30f) * d,
        rnd.nextFloat() * 6.28f, rnd.nextFloat() < 0.22f,
    )

    override fun onDraw(c: Canvas) {
        val simdi = System.nanoTime()
        val dt = if (son == 0L) 0f else ((simdi - son) / 1e9f).coerceAtMost(0.05f)
        son = simdi
        zaman += dt

        boya.style = Paint.Style.FILL
        boya.shader = gradyan
        c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), boya)
        boya.shader = null

        for (k in liste) {
            k.y -= k.hiz * dt
            if (k.y < -k.r * 2) {
                k.y = height + k.r * 2
                k.x = rnd.nextFloat() * width
            }
            val x = k.x + sin(zaman * 1.1f + k.faz) * 10f * d
            if (k.altin) {
                boya.style = Paint.Style.FILL
                boya.color = 0x99B8860B.toInt()
                c.drawCircle(x, k.y, k.r, boya)
                boya.color = 0x99FFC83D.toInt()
                c.drawCircle(x, k.y, k.r * 0.82f, boya)
            } else {
                boya.style = Paint.Style.FILL
                boya.color = 0x18FFFFFF
                c.drawCircle(x, k.y, k.r, boya)
                boya.style = Paint.Style.STROKE
                boya.strokeWidth = 1.5f * d
                boya.color = 0x55FFFFFF
                c.drawCircle(x, k.y, k.r, boya)
                boya.style = Paint.Style.FILL
                boya.color = 0x77FFFFFF
                c.drawCircle(x - k.r * 0.35f, k.y - k.r * 0.35f, k.r * 0.2f, boya)
            }
        }
        postInvalidateOnAnimation()
    }
}
