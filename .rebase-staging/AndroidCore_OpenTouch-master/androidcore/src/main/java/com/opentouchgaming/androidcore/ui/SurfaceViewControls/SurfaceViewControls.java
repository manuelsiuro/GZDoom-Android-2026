package com.opentouchgaming.androidcore.ui.SurfaceViewControls;

import android.content.Context;
import android.graphics.PixelFormat;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import org.libsdl.app.NativeLib;

import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class SurfaceViewControls extends GLSurfaceView
{

    private final MyRenderer mRenderer;

    public SurfaceViewControls(Context context)
    {
        super(context);
        // Create an OpenGL ES 2.0 context
        setEGLContextClientVersion(2);

        // Keep context when minimising
        setPreserveEGLContextOnPause(true);

        // 2. Request an 8-bit RGBA buffer, plus 16-bit depth (no stencil)
        setEGLConfigChooser(8, 8, 8, 8, 16, 0);

        // 3. Make the surface view itself translucent
        getHolder().setFormat(PixelFormat.TRANSLUCENT);
        setZOrderOnTop(true);

        // Set the Renderer for drawing on the GLSurfaceView
        mRenderer = new MyRenderer();
        setRenderer(mRenderer);
        // Render the view continuously (alternatively, RENDERMODE_WHEN_DIRTY)
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    private static class MyRenderer implements GLSurfaceView.Renderer
    {
        // Square vertex coordinates (x, y, z)
        private final float[] squareCoords = {0.5f, 0.5f, 0.0f,   // top right
                -0.5f, 0.5f, 0.0f,   // top left
                -0.5f, -0.5f, 0.0f,   // bottom left
                0.5f, -0.5f, 0.0f    // bottom right
        };
        private final int COORDS_PER_VERTEX = 3;
        private final int vertexCount = squareCoords.length / COORDS_PER_VERTEX;
        private final int vertexStride = COORDS_PER_VERTEX * 4; // 4 bytes per float
        // simple vertex shader
        private final String vertexShaderCode = "attribute vec4 vPosition;" + "void main() {" + "  gl_Position = vPosition;" + "}";
        // simple fragment shader: solid color
        private final String fragmentShaderCode = "precision mediump float;" + "uniform vec4 vColor;" + "void main() {" + "  gl_FragColor = vColor;" + "}";
        // square color: RGBA (red, green, blue, alpha)
        private final float[] color = {0.2f, 0.7f, 0.3f, 1.0f};
        boolean waited = false;
        int width, height;
        private FloatBuffer vertexBuffer;
        private int mProgram;
        private int positionHandle;
        private int colorHandle;

        /**
         * Utility method for compiling a shader.
         */
        private static int loadShader(int type, String shaderCode)
        {
            int shader = GLES20.glCreateShader(type);
            GLES20.glShaderSource(shader, shaderCode);
            GLES20.glCompileShader(shader);
            return shader;
        }

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config)
        {

            // Set the background frame color
            GLES20.glClearColor(0f, 0f, 0f, 0f);
/*
            // initialize vertex byte buffer for shape coordinates
            ByteBuffer bb = ByteBuffer.allocateDirect(squareCoords.length * 4);
            bb.order(ByteOrder.nativeOrder());
            vertexBuffer = bb.asFloatBuffer();
            vertexBuffer.put(squareCoords);
            vertexBuffer.position(0);

            // compile shaders and link program
            int vertexShader   = loadShader(GLES20.GL_VERTEX_SHADER,   vertexShaderCode);
            int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);
            mProgram = GLES20.glCreateProgram();
            GLES20.glAttachShader(mProgram, vertexShader);
            GLES20.glAttachShader(mProgram, fragmentShader);
            GLES20.glLinkProgram(mProgram);

             */
        }

        @Override
        public void onDrawFrame(javax.microedition.khronos.opengles.GL10 unused)
        {
            GLES20.glClearColor(0f, 0f, 0f, 0f);
            GLES20.glViewport(0, 0, width, height);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            GLES20.glEnable(GLES20.GL_BLEND);
            GLES20.glDisable(GLES20.GL_DEPTH_TEST);
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

            NativeLib.renderControls();
/*
            // Redraw background
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

            // Add program to OpenGL ES environment
            GLES20.glUseProgram(mProgram);

            // get handle to vertex shader's vPosition member
            positionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
            // enable a handle to the square vertices
            GLES20.glEnableVertexAttribArray(positionHandle);
            // prepare the square coordinate data
            GLES20.glVertexAttribPointer(
                    positionHandle,
                    COORDS_PER_VERTEX,
                    GLES20.GL_FLOAT,
                    false,
                    vertexStride,
                    vertexBuffer
                                        );

            // get handle to fragment shader's vColor member
            colorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");
            // set color for drawing the square
            GLES20.glUniform4fv(colorHandle, 1, color, 0);

            // draw the square as a triangle strip (4 vertices)
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, vertexCount);

            // disable vertex array
            GLES20.glDisableVertexAttribArray(positionHandle);

 */


        }

        @Override
        public void onSurfaceChanged(javax.microedition.khronos.opengles.GL10 unused, int width, int height)
        {
            // adjust the viewport based on geometry changes

            this.width = width;
            this.height = height;
            GLES20.glViewport(0, 0, width, height);
        }
    }
}
