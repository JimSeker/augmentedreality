package edu.cs4730.arcoresimpledemo;

/**
 * Created by Seker on 7/2/2015.
 *
 *
 * This code actually will draw a cube.
 *
 * Some of the code is used from https://github.com/christopherperry/cube-rotation
 * and changed up to opengl 3.0
 */

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

public class Cube {
    private int mProgramObject;
    private int mMVPMatrixHandle;
    private int mColorHandle;
    private FloatBuffer mVertices;

    //float[] mvpMatrix;
    private final float[] modelViewMatrix = new float[16];
    private final float[] modelViewProjectionMatrix = new float[16];
    private final float[] modelMatrix = new float[16];

    //initial size of the cube.  set here, so it is easier to change later.
    float size = 0.08f;
    private float mAngle =0;

    //this is the initial data, which will need to translated into the mVertices variable in the consturctor.
    float[] mVerticesData = new float[]{
            ////////////////////////////////////////////////////////////////////
            // FRONT
            ////////////////////////////////////////////////////////////////////
            // Triangle 1
            -size, size, size, // top-left
            -size, -size, size, // bottom-left
            size, -size, size, // bottom-right
            // Triangle 2
            size, -size, size, // bottom-right
            size, size, size, // top-right
            -size, size, size, // top-left
            ////////////////////////////////////////////////////////////////////
            // BACK
            ////////////////////////////////////////////////////////////////////
            // Triangle 1
            -size, size, -size, // top-left
            -size, -size, -size, // bottom-left
            size, -size, -size, // bottom-right
            // Triangle 2
            size, -size, -size, // bottom-right
            size, size, -size, // top-right
            -size, size, -size, // top-left

            ////////////////////////////////////////////////////////////////////
            // LEFT
            ////////////////////////////////////////////////////////////////////
            // Triangle 1
            -size, size, -size, // top-left
            -size, -size, -size, // bottom-left
            -size, -size, size, // bottom-right
            // Triangle 2
            -size, -size, size, // bottom-right
            -size, size, size, // top-right
            -size, size, -size, // top-left
            ////////////////////////////////////////////////////////////////////
            // RIGHT
            ////////////////////////////////////////////////////////////////////
            // Triangle 1
            size, size, -size, // top-left
            size, -size, -size, // bottom-left
            size, -size, size, // bottom-right
            // Triangle 2
            size, -size, size, // bottom-right
            size, size, size, // top-right
            size, size, -size, // top-left

            ////////////////////////////////////////////////////////////////////
            // TOP
            ////////////////////////////////////////////////////////////////////
            // Triangle 1
            -size, size, -size, // top-left
            -size, size, size, // bottom-left
            size, size, size, // bottom-right
            // Triangle 2
            size, size, size, // bottom-right
            size, size, -size, // top-right
            -size, size, -size, // top-left
            ////////////////////////////////////////////////////////////////////
            // BOTTOM
            ////////////////////////////////////////////////////////////////////
            // Triangle 1
            -size, -size, -size, // top-left
            -size, -size, size, // bottom-left
            size, -size, size, // bottom-right
            // Triangle 2
            size, -size, size, // bottom-right
            size, -size, -size, // top-right
            -size, -size, -size // top-left
    };

    float colorcyan[] = myColor.cyan();
    float colorblue[] = myColor.blue();
    float colorred[] = myColor.red();
    float colorgray[] = myColor.gray();
    float colorgreen[] = myColor.green();
    float coloryellow[] = myColor.yellow();

    //vertex shader code
    String vShaderStr =
            "#version 300 es 			  \n"
                    + "uniform mat4 uMVPMatrix;     \n"
                    + "in vec4 vPosition;           \n"
                    + "void main()                  \n"
                    + "{                            \n"
                    + "   gl_Position = uMVPMatrix * vPosition;  \n"
                    + "}                            \n";
    //fragment shader code.
    String fShaderStr =
            "#version 300 es		 			          	\n"
                    + "precision mediump float;					  	\n"
                    + "uniform vec4 vColor;	 			 		  	\n"
                    + "out vec4 fragColor;	 			 		  	\n"
                    + "void main()                                  \n"
                    + "{                                            \n"
                    + "  fragColor = vColor;                    	\n"
                    + "}                                            \n";

    String TAG = "Cube";


    //finally some methods
    //constructor
    public Cube() {
        //first setup the mVertices correctly.
        mVertices = ByteBuffer
                .allocateDirect(mVerticesData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(mVerticesData);
        mVertices.position(0);

        //setup the shaders
        int vertexShader;
        int fragmentShader;
        int programObject;
        int[] linked = new int[1];

        // Load the vertex/fragment shaders
        vertexShader = ShaderUtil.LoadShader(GLES20.GL_VERTEX_SHADER, vShaderStr);
        fragmentShader = ShaderUtil.LoadShader(GLES20.GL_FRAGMENT_SHADER, fShaderStr);

        // Create the program object
        programObject = GLES20.glCreateProgram();

        if (programObject == 0) {
            Log.e(TAG, "So some kind of error, but what?");
            return;
        }

        GLES20.glAttachShader(programObject, vertexShader);
        GLES20.glAttachShader(programObject, fragmentShader);

        // Bind vPosition to attribute 0
        GLES20.glBindAttribLocation(programObject, 0, "vPosition");

        // Link the program
        GLES20.glLinkProgram(programObject);

        // Check the link status
        GLES20.glGetProgramiv(programObject, GLES20.GL_LINK_STATUS, linked, 0);

        if (linked[0] == 0) {
            Log.e(TAG, "Error linking program:");
            Log.e(TAG, GLES20.glGetProgramInfoLog(programObject));
            GLES20.glDeleteProgram(programObject);
            return;
        }

        // Store the program object
        mProgramObject = programObject;

        //now everything is setup and ready to draw.
        Matrix.setIdentityM(modelMatrix, 0);
    }

    /**
     * Updates the object model matrix and applies scaling.
     *
     * @param modelMatrix A 4x4 model-to-world transformation matrix, stored in column-major order.
     * @param scaleFactor A separate scaling factor to apply before the {@code modelMatrix}.
     * @see android.opengl.Matrix
     */
    public void updateModelMatrix(float[] modelMatrix, float scaleFactor) {
        float[] scaleMatrix = new float[16];
        Matrix.setIdentityM(scaleMatrix, 0);
        scaleMatrix[0] = scaleFactor;
        scaleMatrix[5] = scaleFactor;
        scaleMatrix[10] = scaleFactor;
        Matrix.multiplyMM(this.modelMatrix, 0, modelMatrix, 0, scaleMatrix, 0);
        //now how it turn.

        //mAngle is how fast, x,y,z which directions it rotates.
        Matrix.rotateM(this.modelMatrix, 0, mAngle, 1.0f, 1.0f, 1.0f);
        //change the angle, so the cube will spin.
        mAngle+=.4;
    }



    public void draw(float[] cameraView, float[] cameraPerspective) {

        // Build the ModelView and ModelViewProjection matrices
        // for calculating object position and light.
        Matrix.multiplyMM(modelViewMatrix, 0, cameraView, 0, modelMatrix, 0);
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, cameraPerspective, 0, modelViewMatrix, 0);

        // Use the program object
        GLES20.glUseProgram(mProgramObject);

        // get handle to shape's transformation matrix
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgramObject, "uMVPMatrix");
        ShaderUtil.checkGlError("glGetUniformLocation");

        // get handle to fragment shader's vColor member
        mColorHandle = GLES20.glGetUniformLocation(mProgramObject, "vColor");


        // Apply the projection and view transformation
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, modelViewProjectionMatrix, 0);
        ShaderUtil.checkGlError("glUniformMatrix4fv");

        int VERTEX_POS_INDX = 0;
        mVertices.position(VERTEX_POS_INDX);  //just in case.  We did it already though.

        //add all the points to the space, so they can be correct by the transformations.
        //would need to do this even if there were no transformations actually.
        GLES20.glVertexAttribPointer(VERTEX_POS_INDX, 3, GLES20.GL_FLOAT,
                false, 0, mVertices);
        GLES20.glEnableVertexAttribArray(VERTEX_POS_INDX);

        //Now we are ready to draw the cube finally.
        int startPos =0;
        int verticesPerface = 6;

        //draw front face
        GLES20.glUniform4fv(mColorHandle, 1, colorblue, 0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES,startPos,verticesPerface);
        startPos += verticesPerface;

        //draw back face
        GLES20.glUniform4fv(mColorHandle, 1, colorcyan, 0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, startPos, verticesPerface);
        startPos += verticesPerface;

        //draw left face
        GLES20.glUniform4fv(mColorHandle, 1, colorred, 0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES,startPos,verticesPerface);
        startPos += verticesPerface;

        //draw right face
        GLES20.glUniform4fv(mColorHandle, 1, colorgray, 0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES,startPos,verticesPerface);
        startPos += verticesPerface;

        //draw top face
        GLES20.glUniform4fv(mColorHandle, 1, colorgreen, 0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES,startPos,verticesPerface);
        startPos += verticesPerface;

        //draw bottom face
        GLES20.glUniform4fv(mColorHandle, 1, coloryellow, 0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES,startPos,verticesPerface);
        //last face, so no need to increment.

    }
}
