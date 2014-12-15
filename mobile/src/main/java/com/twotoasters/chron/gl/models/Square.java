package com.twotoasters.chron.gl.models;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.opengles.GL10;

public class Square {

    float matAmbient[] = {0.4f, 0.4f, 0.4f, 1.0f};
    float matDiffuse[] = {0.2f, 0.6f, 0.9f, 1.0f};
    float matSpecular[] = {0.0f, 0.0f, 0.0f, 1.0f};
    float matShininess[] = {0.0f};

    private FloatBuffer mVertexBuffer;
    private FloatBuffer mTexBuffer;
    private FloatBuffer mNormBuffer;
    private ShortBuffer mIndexBuffer;

    private boolean lightingEnabled;

    public Square(boolean lightingEnabled) {
        this.lightingEnabled = lightingEnabled;

        float[] vertices = {
                // X, Y, Z
                -1f,  1f, 0,   // top left
                 1f,  1f, 0,   // top right
                 1f, -1f, 0,   // bottom right
                -1f, -1f, 0    // bottom left
        };

        float[] texCoords = {
                0f, 0f,
                1f, 0f,
                1f, 1f,
                0f, 1f
        };

        float[] normals = {
                0f, 0f, 1f,
                0f, 0f, 1f
        };

        short[] indices = {
                0, 1, 2,
                2, 3, 0
        };

        // Buffers to be passed to gl*Pointer() functions
        // must be direct, i.e., they must be placed on the
        // native heap where the garbage collector cannot
        // move them.
        //
        // Buffers with multi-byte datatypes (e.g., short, int, float)
        // must have their byte order set to native order

        ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length * 4);
        vbb.order(ByteOrder.nativeOrder());
        mVertexBuffer = vbb.asFloatBuffer();
        mVertexBuffer.put(vertices);
        mVertexBuffer.position(0);

        ByteBuffer tbb = ByteBuffer.allocateDirect(texCoords.length * 4);
        tbb.order(ByteOrder.nativeOrder());
        mTexBuffer = tbb.asFloatBuffer();
        mTexBuffer.put(texCoords);
        mTexBuffer.position(0);

        ByteBuffer nbb = ByteBuffer.allocateDirect(texCoords.length * 4);
        nbb.order(ByteOrder.nativeOrder());
        mNormBuffer = nbb.asFloatBuffer();
        mNormBuffer.put(normals);
        mNormBuffer.position(0);

        ByteBuffer ibb = ByteBuffer.allocateDirect(indices.length * 2);
        ibb.order(ByteOrder.nativeOrder());
        mIndexBuffer = ibb.asShortBuffer();
        mIndexBuffer.put(indices);
        mIndexBuffer.position(0);
    }

    public void draw(GL10 gl) {
        gl.glFrontFace(GL10.GL_CW);
        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mVertexBuffer);
        gl.glEnable(GL10.GL_TEXTURE_2D);
        gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, mTexBuffer);

        if (lightingEnabled) {
            gl.glEnable(GL10.GL_LIGHTING);
            gl.glNormalPointer(3, GL10.GL_FLOAT, mNormBuffer);
            gl.glEnable(GL10.GL_COLOR_MATERIAL);
            gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_AMBIENT, matAmbient, 0);
            gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_DIFFUSE, matDiffuse, 0);
            gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_SPECULAR, matSpecular, 0);
            gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_SHININESS, matShininess, 0);
        }

        gl.glDrawElements(GL10.GL_TRIANGLES, 6, GL10.GL_UNSIGNED_SHORT, mIndexBuffer);
    }
}