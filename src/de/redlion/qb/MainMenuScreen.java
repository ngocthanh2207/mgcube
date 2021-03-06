package de.redlion.qb;

import java.io.IOException;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.Array;

public class MainMenuScreen extends DefaultScreen implements InputProcessor {

	float startTime = 0;
	PerspectiveCamera cam;
	Mesh quadModel;
	Mesh blockModel;
	Mesh playerModel;
	Mesh targetModel;
	Mesh worldModel;
	Mesh wireCubeModel;
	Mesh sphereModel;
	Mesh bigMesh;
	float angleX = 0;
	float angleY = 0;
	SpriteBatch batch;
	SpriteBatch bat;
	BitmapFont font;
	BitmapFont selectedFont;
	Array<String> menuItems = new Array<String>();
	int selectedMenuItem = -1;
	SpriteBatch fadeBatch;
	Sprite blackFade;
	Sprite title;
	float fade = 1.0f;
	boolean finished = false;

	BoundingBox button1 = new BoundingBox();
	BoundingBox button2 = new BoundingBox();
	BoundingBox button3 = new BoundingBox();
	BoundingBox button4 = new BoundingBox();
	BoundingBox button5 = new BoundingBox();
	BoundingBox button6 = new BoundingBox();

	Array<Block> blocks = new Array<Block>();
	Array<Renderable> renderObjects = new Array<Renderable>();
	boolean animateWorld = false;
	boolean animatePlayer = false;

	float angleXBack = 0;
	float angleYBack = 0;
	float delta = 0;

	Vector3 xAxis = new Vector3(1, 0, 0);
	Vector3 yAxis = new Vector3(0, 1, 0);
	Vector3 zAxis = new Vector3(0, 0, 1);

	// GLES20
	Matrix4 model = new Matrix4().idt();
	Matrix4 tmp = new Matrix4().idt();
	private ShaderProgram transShader;
	private ShaderProgram bloomShader;
	FrameBuffer frameBuffer;
	FrameBuffer frameBufferVert;

	Vector3 position = new Vector3();
	private boolean exit = false;

	public MainMenuScreen(Game game) {
		super(game);
		Gdx.input.setInputProcessor(this);

		blackFade = new Sprite(new Texture(Gdx.files.internal("data/blackfade.png")));
		
		blockModel = Resources.getInstance().blockModel;
		playerModel = Resources.getInstance().playerModel;
		targetModel = Resources.getInstance().targetModel;
		quadModel = Resources.getInstance().quadModel;
		wireCubeModel = Resources.getInstance().wireCubeModel;
		sphereModel = Resources.getInstance().sphereModel;
		bigMesh = Resources.getInstance().bigMesh;
		
		cam = new PerspectiveCamera(60, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		cam.position.set(5.0f, 0, 16f);
		cam.direction.set(0, 0, -1);
		cam.up.set(0, 1, 0);
		cam.near = 1f;
		cam.far = 1000;

		// controller = new PerspectiveCamController(cam);
		// Gdx.input.setInputProcessor(controller);

		batch = new SpriteBatch();
		batch.getProjectionMatrix().setToOrtho2D(0, 0, 800, 480);
		font = Resources.getInstance().font;
		font.setScale(1);
		font.scale(0.5f);
		selectedFont = Resources.getInstance().selectedFont;
		selectedFont.setScale(1);
		selectedFont.scale(0.5f);

		fadeBatch = new SpriteBatch();
		fadeBatch.getProjectionMatrix().setToOrtho2D(0, 0, 2, 2);

		transShader = Resources.getInstance().transShader;
		bloomShader = Resources.getInstance().bloomShader;

		menuItems.clear();
		if(HighScoreManager.getInstance().getHighScore(1).first==0) {
			menuItems.add("start game");
		} else {
			menuItems.add("resume game");
		}
		menuItems.add("select level");
		menuItems.add("time attack");
		menuItems.add("tutorial");
		menuItems.add("level editor");

		initRender();

		initLevel(0);
		angleY = -70;
		angleX = -10;
		
		button1.set(new Vector3(470, 110, 0), new Vector3(770, 50, 0));
		button2.set(new Vector3(470, 200, 0), new Vector3(770, 140, 0));
		button3.set(new Vector3(470, 290, 0), new Vector3(770, 220, 0));
		button4.set(new Vector3(470, 360, 0), new Vector3(770, 310, 0));
		button5.set(new Vector3(470, 440, 0), new Vector3(770, 380, 0));
		button6.set(new Vector3(040, 440, 0), new Vector3(250, 380, 0));
		
		exit = false;
	}

	public void initRender() {
		Gdx.graphics.getGL20().glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

		frameBuffer = new FrameBuffer(Format.RGB565, Resources.getInstance().m_i32TexSize, Resources.getInstance().m_i32TexSize, false);
		frameBufferVert = new FrameBuffer(Format.RGB565, Resources.getInstance().m_i32TexSize, Resources.getInstance().m_i32TexSize, false);

		frameBuffer.getColorBufferTexture().setFilter(TextureFilter.Linear, TextureFilter.Linear);
		frameBufferVert.getColorBufferTexture().setFilter(TextureFilter.Linear, TextureFilter.Linear);
		
		Gdx.gl.glClearColor(Resources.getInstance().clearColor[0],Resources.getInstance().clearColor[1],Resources.getInstance().clearColor[2],Resources.getInstance().clearColor[3]);
		Gdx.graphics.getGL20().glDepthMask(true);
		Gdx.graphics.getGL20().glColorMask(true, true, true, true);
	}

	@Override
	public void resize(int width, int height) {
		super.resize(width, height);
		cam = new PerspectiveCamera(60, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		cam.position.set(5.0f, 0, 16f);
		cam.direction.set(0, 0, -1);
		cam.up.set(0, 1, 0);
		cam.near = 1f;
		cam.far = 1000;
		initRender();
	}

	private void initLevel(int levelnumber) {
		renderObjects.clear();
		blocks.clear();

		int[][][] level = Resources.getInstance().opening;

		// finde player pos
		int z = 0, y = 0, x = 0;
		for (z = 0; z < 10; z++) {
			for (y = 0; y < 10; y++) {
				for (x = 0; x < 10; x++) {
					if (level[z][y][x] == 1) {
						blocks.add(new Block(new Vector3(10f - (x * 2), -10f + (y * 2), -10f + (z * 2))));
					}
//					if (level[z][y][x] == 2) {
//						player.position.x = -10f + (x * 2);
//						player.position.y = -10f + (y * 2);
//						player.position.z = -10f + (z * 2);
//					}
//					if (level[z][y][x] == 3) {
//						target.position.x = -10f + (x * 2);
//						target.position.y = -10f + (y * 2);
//						target.position.z = -10f + (z * 2);
//					}
//					if (level[z][y][x] >= 4 && level[z][y][x] <= 8) {
//						Portal temp = new Portal(level[z][y][x]);
//						temp.position.x = -10f + (x * 2);
//						temp.position.y = -10f + (y * 2);
//						temp.position.z = -10f + (z * 2);
//						portals.add(temp);
//					}
//					if (level[z][y][x] >= -8 && level[z][y][x] <= -4) {
//						Portal temp = new Portal(level[z][y][x]);
//						temp.position.x = -10f + (x * 2);
//						temp.position.y = -10f + (y * 2);
//						temp.position.z = -10f + (z * 2);
//						portals.add(temp);
//					}
//					if (level[z][y][x] == 9) {
//						MovableBlock temp = new MovableBlock(new Vector3(-10f + (x * 2), -10f + (y * 2), -10f + (z * 2)));
//						movableBlocks.add(temp);
//					}
				}
			}
		}

		// renderObjects.add(player);
		// renderObjects.add(target);
		renderObjects.addAll(blocks);
//		renderObjects.addAll(portals);
//		renderObjects.addAll(movableBlocks);
	}

	@Override
	public void show() {
	}	
	
//	@Override
//	public void render(float deltaTime) {
//		accumulator += Gdx.graphics.getDeltaTime();
//		while(accumulator > 1.0f / 60.0f) { 
//		fixedTimeStepRender();
//		accumulator -= 1.0f / 60.0f;
//		}
//		
//	}
	
	@Override
	public void render(float deltaTime) {		
		delta = Math.min(0.02f, deltaTime);
		startTime += delta;

		angleXBack += MathUtils.sin(startTime) * delta* 10f;
		angleYBack += MathUtils.cos(startTime) *delta* 5f;

		angleX += MathUtils.sin(startTime) *delta* 10f;
		angleY += MathUtils.cos(startTime) *delta* 5f;

		cam.update();

		sortScene();

		// render scene again
		renderScene();
		renderMenu();
		
		if(Resources.getInstance().bloomOnOff) {
			frameBuffer.begin();
			renderScene();
			renderMenu();
			frameBuffer.end();
	
			// PostProcessing
			Gdx.gl.glDisable(GL20.GL_CULL_FACE);
			Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
			Gdx.gl.glDisable(GL20.GL_BLEND);
	
			bloomShader.begin();
			
			frameBuffer.getColorBufferTexture().bind(0);
	
			bloomShader.setUniformi("sTexture", 0);
			bloomShader.setUniformf("bloomFactor", Helper.map((MathUtils.sin(startTime * 3f) * 0.5f) + 0.5f,0,1,0.67f,0.75f));
	
			frameBufferVert.begin();
			bloomShader.setUniformf("TexelOffsetX", Resources.getInstance().m_fTexelOffset);
			bloomShader.setUniformf("TexelOffsetY", 0.0f);			
			quadModel.render(bloomShader, GL20.GL_TRIANGLE_STRIP);
			frameBufferVert.end();
	
			frameBufferVert.getColorBufferTexture().bind(0);
	
			frameBuffer.begin();
			bloomShader.setUniformf("TexelOffsetX", 0.0f);
			bloomShader.setUniformf("TexelOffsetY", Resources.getInstance().m_fTexelOffset);
			quadModel.render(bloomShader, GL20.GL_TRIANGLE_STRIP);			
			frameBuffer.end();
			
			frameBuffer.getColorBufferTexture().bind(0);
	
			frameBufferVert.begin();
			bloomShader.setUniformf("TexelOffsetX", Resources.getInstance().m_fTexelOffset/2);
			bloomShader.setUniformf("TexelOffsetY", Resources.getInstance().m_fTexelOffset/2);			
			quadModel.render(bloomShader, GL20.GL_TRIANGLE_STRIP);
			frameBufferVert.end();
					
			bloomShader.end();
			
			batch.setBlendFunction(GL20.GL_ONE, GL20.GL_ONE);
			batch.getProjectionMatrix().setToOrtho2D(0, 0, Resources.getInstance().m_i32TexSize, Resources.getInstance().m_i32TexSize);
			batch.begin();
			batch.draw(frameBufferVert.getColorBufferTexture(),0,0);
			batch.end();
			batch.getProjectionMatrix().setToOrtho2D(0, 0, 800,480);
			
			if(Gdx.graphics.getBufferFormat().coverageSampling) {
				Gdx.gl.glClear(GL20.GL_COVERAGE_BUFFER_BIT_NV);
				Gdx.graphics.getGL20().glColorMask(false, false, false, false);			
				renderScene();
				renderMenu();
				Gdx.graphics.getGL20().glColorMask(true, true, true, true);
				
				Gdx.gl.glDisable(GL20.GL_CULL_FACE);
				Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
				Gdx.gl.glDisable(GL20.GL_BLEND);
			}

		} else {
			Gdx.gl.glDisable(GL20.GL_CULL_FACE);
			Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
			Gdx.gl.glDisable(GL20.GL_BLEND);
		}	

		batch.begin();
		float y = 405;
		for (String s : menuItems) {
			if (selectedMenuItem<5 && selectedMenuItem > -1 && s.equals(menuItems.get(selectedMenuItem)))
				selectedFont.draw(batch, s, 480, y);
			else
				font.draw(batch, s, 480, y);
			y -= 80;
		}
		
		if (selectedMenuItem == 5)
			selectedFont.draw(batch, "Options", 50, 80);
		else
			font.draw(batch, "Options",50, 80);
		
		batch.end();

		if (!finished && fade > 0) {
			fade = Math.max(fade - (delta*2.f), 0);
			fadeBatch.begin();
			blackFade.setColor(blackFade.getColor().r, blackFade.getColor().g, blackFade.getColor().b, fade);
			blackFade.draw(fadeBatch);
			fadeBatch.end();
		}

		if (finished) {
			fade = Math.min(fade + (delta*2.f), 1);
			fadeBatch.begin();
			blackFade.setColor(blackFade.getColor().r, blackFade.getColor().g, blackFade.getColor().b, fade);
			blackFade.draw(fadeBatch);
			fadeBatch.end();
			if (fade >= 1) {
				switch (selectedMenuItem) {
				case 0:
					//search level which has a highscore
					boolean found = false;
					for(HighScore highscore:HighScoreManager.getInstance().highscores) {
						if(highscore.first==0) {
							game.setScreen(new GameScreen(game,highscore.level,0));
							found = true;
							break;
						}
					}		
					//all played?
					if(found==false) {
						game.setScreen(new GameScreen(game,1,0));
					}
					break;
				case 1:
					game.setScreen(new LevelSelectScreen(game,0));
					break;
				case 2:
					game.setScreen(new GameScreen(game,1,1));
					break;
				case 3:
					game.setScreen(new LevelSelectScreen(game,1));
					break;
				case 4:
					game.setScreen(new LevelSelectScreen(game,2));
					break;
				case 5:
					game.setScreen(new OptionsScreen(game));
					break;

				default:
					break;
				}
				
			}
		}

		
		if(exit == true) {
			if(!(Gdx.app.getType() == ApplicationType.Applet)) {
				Gdx.app.exit();
			}
		}
	}

	private void sortScene() {
		// sort blocks because of transparency
		for (int i = 0; i < renderObjects.size; i++) {
			tmp.idt();
			model.idt();

			tmp.setToScaling(0.5f, 0.5f, 0.5f);
			model.mul(tmp);

			tmp.setToRotation(xAxis, angleX);
			model.mul(tmp);
			tmp.setToRotation(yAxis, angleY);
			model.mul(tmp);

			tmp.setToTranslation(renderObjects.get(i).position.x, renderObjects.get(i).position.y, renderObjects.get(i).position.z);
			model.mul(tmp);

			tmp.setToScaling(0.95f, 0.95f, 0.95f);
			model.mul(tmp);

			model.getTranslation(position);

			renderObjects.get(i).model.set(model);

			renderObjects.get(i).sortPosition = cam.position.dst(position);
		}
		renderObjects.sort();
	}

	private void renderMenu() {

		Gdx.gl.glEnable(GL20.GL_CULL_FACE);

		Gdx.gl20.glEnable(GL20.GL_BLEND);
		Gdx.gl20.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

		transShader.begin();
		transShader.setUniformMatrix("VPMatrix", cam.combined);

		Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
		{
			// render Button 1
			tmp.idt();
			model.idt();

			tmp.setToScaling(3.5f, 0.6f, 0.5f);
			model.mul(tmp);

			tmp.setToRotation(xAxis, (angleXBack / 40.f));
			model.mul(tmp);
			tmp.setToRotation(yAxis, (angleYBack / 100.f) - 2.f);
			model.mul(tmp);

			tmp.setToTranslation(3.3f, 6.0f, 12);
			model.mul(tmp);

			transShader.setUniformMatrix("MMatrix", model);

			if(selectedMenuItem==0) {
				transShader.setUniformf("a_color",Resources.getInstance().blockEdgeColor[0],Resources.getInstance().blockEdgeColor[1],Resources.getInstance().blockEdgeColor[2],Resources.getInstance().blockEdgeColor[3]+0.2f);
				wireCubeModel.render(transShader, GL20.GL_LINE_STRIP);
	
				transShader.setUniformf("a_color", Resources.getInstance().blockColor[0],Resources.getInstance().blockColor[1],Resources.getInstance().blockColor[2],Resources.getInstance().blockColor[3]+0.2f);
				blockModel.render(transShader, GL20.GL_TRIANGLES);
			} else {
				transShader.setUniformf("a_color",Resources.getInstance().blockEdgeColor[0],Resources.getInstance().blockEdgeColor[1],Resources.getInstance().blockEdgeColor[2],Resources.getInstance().blockEdgeColor[3]-0.1f);
				wireCubeModel.render(transShader, GL20.GL_LINE_STRIP);
	
				transShader.setUniformf("a_color", Resources.getInstance().blockColor[0],Resources.getInstance().blockColor[1],Resources.getInstance().blockColor[2],Resources.getInstance().blockColor[3]-0.1f);
				blockModel.render(transShader, GL20.GL_TRIANGLES);
			}
		}

		{
			// render Button 2
			tmp.idt();
			model.idt();

			tmp.setToScaling(3.5f, 0.6f, 0.5f);
			model.mul(tmp);

			tmp.setToRotation(xAxis, (angleXBack / 40.f));
			model.mul(tmp);
			tmp.setToRotation(yAxis, (angleYBack / 100.f) - 2.f);
			model.mul(tmp);

			tmp.setToTranslation(3.3f, 2.9f, 12);
			model.mul(tmp);

			transShader.setUniformMatrix("MMatrix", model);

			if(selectedMenuItem==1) {
				transShader.setUniformf("a_color",Resources.getInstance().blockEdgeColor[0],Resources.getInstance().blockEdgeColor[1],Resources.getInstance().blockEdgeColor[2],Resources.getInstance().blockEdgeColor[3]+0.2f);
				wireCubeModel.render(transShader, GL20.GL_LINE_STRIP);
	
				transShader.setUniformf("a_color", Resources.getInstance().blockColor[0],Resources.getInstance().blockColor[1],Resources.getInstance().blockColor[2],Resources.getInstance().blockColor[3]+0.2f);
				blockModel.render(transShader, GL20.GL_TRIANGLES);
			} else {
				transShader.setUniformf("a_color",Resources.getInstance().blockEdgeColor[0],Resources.getInstance().blockEdgeColor[1],Resources.getInstance().blockEdgeColor[2],Resources.getInstance().blockEdgeColor[3]-0.1f);
				wireCubeModel.render(transShader, GL20.GL_LINE_STRIP);
	
				transShader.setUniformf("a_color", Resources.getInstance().blockColor[0],Resources.getInstance().blockColor[1],Resources.getInstance().blockColor[2],Resources.getInstance().blockColor[3]-0.1f);
				blockModel.render(transShader, GL20.GL_TRIANGLES);
			}
		}

		{
			// render Button 3
			tmp.idt();
			model.idt();

			tmp.setToScaling(3.5f, 0.6f, 0.5f);
			model.mul(tmp);

			tmp.setToRotation(xAxis, (angleXBack / 40.f));
			model.mul(tmp);
			tmp.setToRotation(yAxis, (angleYBack / 100.f) - 2.f);
			model.mul(tmp);

			tmp.setToTranslation(3.3f, -0.3f, 12);
			model.mul(tmp);

			transShader.setUniformMatrix("MMatrix", model);

			if(selectedMenuItem==2) {
				transShader.setUniformf("a_color",Resources.getInstance().blockEdgeColor[0],Resources.getInstance().blockEdgeColor[1],Resources.getInstance().blockEdgeColor[2],Resources.getInstance().blockEdgeColor[3]+0.2f);
				wireCubeModel.render(transShader, GL20.GL_LINE_STRIP);
	
				transShader.setUniformf("a_color", Resources.getInstance().blockColor[0],Resources.getInstance().blockColor[1],Resources.getInstance().blockColor[2],Resources.getInstance().blockColor[3]+0.2f);
				blockModel.render(transShader, GL20.GL_TRIANGLES);
			} else {
				transShader.setUniformf("a_color",Resources.getInstance().blockEdgeColor[0],Resources.getInstance().blockEdgeColor[1],Resources.getInstance().blockEdgeColor[2],Resources.getInstance().blockEdgeColor[3]-0.1f);
				wireCubeModel.render(transShader, GL20.GL_LINE_STRIP);
	
				transShader.setUniformf("a_color", Resources.getInstance().blockColor[0],Resources.getInstance().blockColor[1],Resources.getInstance().blockColor[2],Resources.getInstance().blockColor[3]-0.1f);
				blockModel.render(transShader, GL20.GL_TRIANGLES);
			}
		}

		{
			// render Button 4
			tmp.idt();
			model.idt();

			tmp.setToScaling(3.5f, 0.6f, 0.5f);
			model.mul(tmp);

			tmp.setToRotation(xAxis, (angleXBack / 40.f));
			model.mul(tmp);
			tmp.setToRotation(yAxis, (angleYBack / 100.f) - 2.f);
			model.mul(tmp);

			tmp.setToTranslation(3.3f, -3.5f, 12);
			model.mul(tmp);

			transShader.setUniformMatrix("MMatrix", model);

			if(selectedMenuItem==3) {
				transShader.setUniformf("a_color",Resources.getInstance().blockEdgeColor[0],Resources.getInstance().blockEdgeColor[1],Resources.getInstance().blockEdgeColor[2],Resources.getInstance().blockEdgeColor[3]+0.2f);
				wireCubeModel.render(transShader, GL20.GL_LINE_STRIP);
	
				transShader.setUniformf("a_color", Resources.getInstance().blockColor[0],Resources.getInstance().blockColor[1],Resources.getInstance().blockColor[2],Resources.getInstance().blockColor[3]+0.2f);
				blockModel.render(transShader, GL20.GL_TRIANGLES);
			} else {
				transShader.setUniformf("a_color",Resources.getInstance().blockEdgeColor[0],Resources.getInstance().blockEdgeColor[1],Resources.getInstance().blockEdgeColor[2],Resources.getInstance().blockEdgeColor[3]-0.1f);
				wireCubeModel.render(transShader, GL20.GL_LINE_STRIP);
	
				transShader.setUniformf("a_color", Resources.getInstance().blockColor[0],Resources.getInstance().blockColor[1],Resources.getInstance().blockColor[2],Resources.getInstance().blockColor[3]-0.1f);
				blockModel.render(transShader, GL20.GL_TRIANGLES);
			}
		}
		
		{
			// render Button 5
			tmp.idt();
			model.idt();

			tmp.setToScaling(3.5f, 0.6f, 0.5f);
			model.mul(tmp);

			tmp.setToRotation(xAxis, (angleXBack / 40.f));
			model.mul(tmp);
			tmp.setToRotation(yAxis, (angleYBack / 100.f) - 2.f);
			model.mul(tmp);

			tmp.setToTranslation(3.3f, -6.6f, 12);
			model.mul(tmp);

			transShader.setUniformMatrix("MMatrix", model);

			if(selectedMenuItem==4) {
				transShader.setUniformf("a_color",Resources.getInstance().blockEdgeColor[0],Resources.getInstance().blockEdgeColor[1],Resources.getInstance().blockEdgeColor[2],Resources.getInstance().blockEdgeColor[3]+0.2f);
				wireCubeModel.render(transShader, GL20.GL_LINE_STRIP);
	
				transShader.setUniformf("a_color", Resources.getInstance().blockColor[0],Resources.getInstance().blockColor[1],Resources.getInstance().blockColor[2],Resources.getInstance().blockColor[3]+0.2f);
				blockModel.render(transShader, GL20.GL_TRIANGLES);
			} else {
				transShader.setUniformf("a_color",Resources.getInstance().blockEdgeColor[0],Resources.getInstance().blockEdgeColor[1],Resources.getInstance().blockEdgeColor[2],Resources.getInstance().blockEdgeColor[3]-0.1f);
				wireCubeModel.render(transShader, GL20.GL_LINE_STRIP);
	
				transShader.setUniformf("a_color", Resources.getInstance().blockColor[0],Resources.getInstance().blockColor[1],Resources.getInstance().blockColor[2],Resources.getInstance().blockColor[3]-0.1f);
				blockModel.render(transShader, GL20.GL_TRIANGLES);
			}
		}
		
//		{
//			// render Button 6
//			tmp.idt();
//			model.idt();
//
//			tmp.setToScaling(2.5f, 0.6f, 0.5f);
//			model.mul(tmp);
//
//			tmp.setToRotation(xAxis, (angleXBack / 40.f));
//			model.mul(tmp);
//			tmp.setToRotation(yAxis, (angleYBack / 100.f) + 8.f);
//			model.mul(tmp);
//
//			tmp.setToTranslation(-2.2f, -6.6f, 12);
//			model.mul(tmp);
//
//			transShader.setUniformMatrix("MMatrix", model);
//
//			if(selectedMenuItem==5) {
//				transShader.setUniformf("a_color",Resources.getInstance().blockEdgeColor[0],Resources.getInstance().blockEdgeColor[1],Resources.getInstance().blockEdgeColor[2],Resources.getInstance().blockEdgeColor[3]+0.2f);
//				wireCubeModel.render(transShader, GL20.GL_LINE_STRIP);
//	
//				transShader.setUniformf("a_color", Resources.getInstance().blockColor[0],Resources.getInstance().blockColor[1],Resources.getInstance().blockColor[2],Resources.getInstance().blockColor[3]+0.2f);
//				blockModel.render(transShader, GL20.GL_TRIANGLES);
//			} else {
//				transShader.setUniformf("a_color",Resources.getInstance().blockEdgeColor[0],Resources.getInstance().blockEdgeColor[1],Resources.getInstance().blockEdgeColor[2],Resources.getInstance().blockEdgeColor[3]-0.3f);
//				wireCubeModel.render(transShader, GL20.GL_LINE_STRIP);
//	
//				transShader.setUniformf("a_color", Resources.getInstance().blockColor[0],Resources.getInstance().blockColor[1],Resources.getInstance().blockColor[2],Resources.getInstance().blockColor[3]-0.2f);
//				blockModel.render(transShader, GL20.GL_TRIANGLES);
//			}
//		}

		transShader.end();
	}

	private void renderScene() {		
		Gdx.gl.glEnable(GL20.GL_CULL_FACE);
		
		Gdx.gl20.glEnable(GL20.GL_BLEND);
		Gdx.gl20.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
				
		transShader.begin();

		
		transShader.setUniformMatrix("VPMatrix", cam.combined);
		{
			// render Background Wire
			tmp.idt();
			model.idt();

			tmp.setToScaling(20.5f, 20.5f, 20.5f);
			model.mul(tmp);

			tmp.setToRotation(xAxis, angleX + angleXBack);
			model.mul(tmp);
			tmp.setToRotation(yAxis, angleY + angleYBack);
			model.mul(tmp);

			tmp.setToTranslation(0, 0, 0);
			model.mul(tmp);

			transShader.setUniformMatrix("MMatrix", model);
			
			transShader.setUniformf("a_color", Resources.getInstance().backgroundWireColor[0],Resources.getInstance().backgroundWireColor[1],Resources.getInstance().backgroundWireColor[2],Resources.getInstance().backgroundWireColor[3]);
//			bigMesh.render(transShader, GL20.GL_LINE_STRIP, 0, Resources.getInstance().bigMeshVerticesCntSubMesh.get(0));
//Gdx.app.log("", Resources.getInstance().bigMeshVerticesCntSubMesh.get(1)+ " " + (Resources.getInstance().bigMeshVerticesCntSubMesh.get(2)-Resources.getInstance().bigMeshVerticesCntSubMesh.get(1)));
			playerModel.render(transShader, GL20.GL_LINE_STRIP);
		}
		{
			// render Wire
			tmp.idt();
			model.idt();

			tmp.setToScaling(5.5f, 5.5f, 5.5f);
			model.mul(tmp);

			tmp.setToRotation(xAxis, angleX);
			model.mul(tmp);
			tmp.setToRotation(yAxis, angleY);
			model.mul(tmp);

			tmp.setToTranslation(0, 0, 0);
			model.mul(tmp);

			transShader.setUniformMatrix("MMatrix", model);

			transShader.setUniformf("a_color", Resources.getInstance().clearColor[0],Resources.getInstance().clearColor[1],Resources.getInstance().clearColor[2],Resources.getInstance().clearColor[3]);
//			bigMesh.render(transShader, GL20.GL_TRIANGLES, 0, Resources.getInstance().bigMeshVerticesCntSubMesh.get(0));
			blockModel.render(transShader, GL20.GL_TRIANGLES);
			
			transShader.setUniformf("a_color", Resources.getInstance().wireCubeEdgeColor[0],Resources.getInstance().wireCubeEdgeColor[1],Resources.getInstance().wireCubeEdgeColor[2],Resources.getInstance().wireCubeEdgeColor[3]);
//			bigMesh.render(transShader, GL20.GL_LINE_STRIP, Resources.getInstance().bigMeshVerticesCntSubMesh.get(3), Resources.getInstance().bigMeshVerticesCntSubMesh.get(4)-Resources.getInstance().bigMeshVerticesCntSubMesh.get(3));
			wireCubeModel.render(transShader, GL20.GL_LINE_STRIP);
			
//			transShader.setUniformf("a_color", Resources.getInstance().wireCubeColor[0],Resources.getInstance().wireCubeColor[1],Resources.getInstance().wireCubeColor[2],Resources.getInstance().wireCubeColor[3]);
//			blockModel.render(transShader, GL20.GL_TRIANGLES);
		}
				
		// render all objects
		for (int i = 0; i<renderObjects.size;++i) {
						
			//render impact
			if(renderObjects.get(i).isCollidedAnimation == true && renderObjects.get(i).collideAnimation == 0) {
				renderObjects.get(i).collideAnimation = 1.0f;
			}
			if(renderObjects.get(i).collideAnimation>0.0f) {
				renderObjects.get(i).collideAnimation -= delta*1.f;
				renderObjects.get(i).collideAnimation = Math.max(0.0f, renderObjects.get(i).collideAnimation);
				if(renderObjects.get(i).collideAnimation == 0.0f) renderObjects.get(i).isCollidedAnimation = false;
			}
			
			
			if(renderObjects.get(i) instanceof Block) {
				model.set(renderObjects.get(i).model);
	
				transShader.setUniformMatrix("MMatrix", model);
	
//				transShader.setUniformf("a_color", Resources.getInstance().blockColor[0]- (Helper.map(renderObjects.get(i).sortPosition,10,25,0,0.4f)), Resources.getInstance().blockColor[1], Resources.getInstance().blockColor[2] + (Helper.map(renderObjects.get(i).sortPosition,10,25,0,0.15f)), Resources.getInstance().blockColor[3]+ renderObjects.get(i).collideAnimation + (Helper.map(renderObjects.get(i).sortPosition,10,25,0.15f,-0.25f)));
//				blockModel.render(transShader, GL20.GL_TRIANGLES);
//				
//				transShader.setUniformf("a_color",Resources.getInstance().blockEdgeColor[0] - (Helper.map(renderObjects.get(i).sortPosition,10,25,0,0.4f)), Resources.getInstance().blockEdgeColor[1],Resources.getInstance().blockEdgeColor[2] + (Helper.map(renderObjects.get(i).sortPosition,10,25,0,0.15f)), Resources.getInstance().blockEdgeColor[3] + renderObjects.get(i).collideAnimation + (Helper.map(renderObjects.get(i).sortPosition,10,25,0.15f,-0.25f)));
//				wireCubeModel.render(transShader, GL20.GL_LINE_STRIP);
				
				transShader.setUniformf("a_color", Resources.getInstance().blockColor[0], Resources.getInstance().blockColor[1], Resources.getInstance().blockColor[2], Resources.getInstance().blockColor[3]+ renderObjects.get(i).collideAnimation );
				blockModel.render(transShader, GL20.GL_TRIANGLES);
				
				transShader.setUniformf("a_color",Resources.getInstance().blockEdgeColor[0], Resources.getInstance().blockEdgeColor[1],Resources.getInstance().blockEdgeColor[2], Resources.getInstance().blockEdgeColor[3] + renderObjects.get(i).collideAnimation );
				wireCubeModel.render(transShader, GL20.GL_LINE_STRIP);
			}
			
			// render movableblocks
			if(renderObjects.get(i) instanceof MovableBlock) {
				model.set(renderObjects.get(i).model);
	
				transShader.setUniformMatrix("MMatrix", model);
	
				transShader.setUniformf("a_color", Resources.getInstance().movableBlockColor[0], Resources.getInstance().movableBlockColor[1], Resources.getInstance().movableBlockColor[2], Resources.getInstance().movableBlockColor[3] + renderObjects.get(i).collideAnimation);
				wireCubeModel.render(transShader, GL20.GL_LINE_STRIP);
	
				transShader.setUniformf("a_color", Resources.getInstance().movableBlockEdgeColor[0], Resources.getInstance().movableBlockEdgeColor[1],Resources.getInstance().movableBlockEdgeColor[2], Resources.getInstance().movableBlockEdgeColor[3] + renderObjects.get(i).collideAnimation);
				blockModel.render(transShader, GL20.GL_TRIANGLES);
			} 
			
			// render switchableblocks
			if(renderObjects.get(i) instanceof SwitchableBlock) {
				if(!((SwitchableBlock) renderObjects.get(i)).isSwitched) {	
					model.set(renderObjects.get(i).model);
		
					transShader.setUniformMatrix("MMatrix", model);
		
					transShader.setUniformf("a_color", Resources.getInstance().switchBlockColor[0] * (Math.abs(((SwitchableBlock)renderObjects.get(i)).id)),Resources.getInstance().switchBlockColor[1]* (Math.abs(((SwitchableBlock)renderObjects.get(i)).id)), Resources.getInstance().switchBlockColor[2] * (Math.abs(((SwitchableBlock)renderObjects.get(i)).id)), Resources.getInstance().switchBlockColor[3]+ renderObjects.get(i).collideAnimation);
					wireCubeModel.render(transShader, GL20.GL_LINE_STRIP);
		
					transShader.setUniformf("a_color", Resources.getInstance().switchBlockEdgeColor[0] * (Math.abs(((SwitchableBlock)renderObjects.get(i)).id)), Resources.getInstance().switchBlockEdgeColor[1]* (Math.abs(((SwitchableBlock)renderObjects.get(i)).id)), Resources.getInstance().switchBlockEdgeColor[2] * (Math.abs(((SwitchableBlock)renderObjects.get(i)).id)), Resources.getInstance().switchBlockEdgeColor[3] + renderObjects.get(i).collideAnimation);
					blockModel.render(transShader, GL20.GL_TRIANGLES);
				}
			}
			
			// render switches
			if(renderObjects.get(i) instanceof Switch) {
				model.set(renderObjects.get(i).model);	

				tmp.setToScaling(0.3f, 0.3f, 0.3f);
				model.mul(tmp);

				transShader.setUniformMatrix("MMatrix", model);
				transShader.setUniformf("a_color", Resources.getInstance().switchBlockColor[0] * (Math.abs(((Switch)renderObjects.get(i)).id)),Resources.getInstance().switchBlockColor[1]* (Math.abs(((Switch)renderObjects.get(i)).id)), Resources.getInstance().switchBlockColor[2] * (Math.abs(((Switch)renderObjects.get(i)).id)), Resources.getInstance().switchBlockColor[3]+ renderObjects.get(i).collideAnimation);
				playerModel.render(transShader, GL20.GL_TRIANGLES);
				
				tmp.setToScaling(2.0f, 2.0f, 2.0f);
				model.mul(tmp);

				//render hull			
				transShader.setUniformMatrix("MMatrix", model);
				transShader.setUniformf("a_color", Resources.getInstance().switchBlockEdgeColor[0] * (Math.abs(((Switch)renderObjects.get(i)).id)), Resources.getInstance().switchBlockEdgeColor[1]* (Math.abs(((Switch)renderObjects.get(i)).id)), Resources.getInstance().switchBlockEdgeColor[2] * (Math.abs(((Switch)renderObjects.get(i)).id)), Resources.getInstance().switchBlockEdgeColor[3] + renderObjects.get(i).collideAnimation);
				playerModel.render(transShader, GL20.GL_LINE_STRIP);
			}
			
			// render Player
			if(renderObjects.get(i) instanceof Player) {
				model.set(renderObjects.get(i).model);	
				
				tmp.setToRotation(xAxis, angleXBack);
				model.mul(tmp);
				tmp.setToRotation(yAxis, angleYBack);
				model.mul(tmp);

				tmp.setToScaling(0.5f, 0.5f, 0.5f);
				model.mul(tmp);

				transShader.setUniformMatrix("MMatrix", model);
				transShader.setUniformf("a_color",Resources.getInstance().playerColor[0], Resources.getInstance().playerColor[1], Resources.getInstance().playerColor[2], Resources.getInstance().playerColor[3]);
				playerModel.render(transShader, GL20.GL_TRIANGLES);
				
				tmp.setToScaling(2.0f, 2.0f, 2.0f);
				model.mul(tmp);

				//render hull			
				transShader.setUniformMatrix("MMatrix", model);
				transShader.setUniformf("a_color",Resources.getInstance().playerEdgeColor[0], Resources.getInstance().playerEdgeColor[1], Resources.getInstance().playerEdgeColor[2], Resources.getInstance().playerEdgeColor[3]);
				playerModel.render(transShader, GL20.GL_LINE_STRIP);
			}
			
			// render Portals
			if(renderObjects.get(i) instanceof Portal) {
				if(renderObjects.get(i).position.x != -11) {
					// render Portal
					model.set(renderObjects.get(i).model);
		
					transShader.setUniformMatrix("MMatrix", model);
					
					transShader.setUniformf("a_color", Resources.getInstance().portalColor[0], Resources.getInstance().portalColor[1] * ((Math.abs(((Portal)renderObjects.get(i)).id)*4f)), Resources.getInstance().portalColor[2], Resources.getInstance().portalColor[3] * (Math.abs(((Portal)renderObjects.get(i)).id)) + renderObjects.get(i).collideAnimation);
					blockModel.render(transShader, GL20.GL_TRIANGLES);
					
					//render hull			
					transShader.setUniformf("a_color", Resources.getInstance().portalEdgeColor[0],Resources.getInstance().portalEdgeColor[1] * ((Math.abs(((Portal)renderObjects.get(i)).id)*4f)), Resources.getInstance().portalEdgeColor[2], Resources.getInstance().portalEdgeColor[3] * (Math.abs(((Portal)renderObjects.get(i)).id)) + renderObjects.get(i).collideAnimation);
					wireCubeModel.render(transShader, GL20.GL_LINE_STRIP);
				}
			}
				
			// render Target
			if(renderObjects.get(i) instanceof Target) {
				model.set(renderObjects.get(i).model);
				
				tmp.setToRotation(yAxis, angleY + angleYBack);
				model.mul(tmp);

				transShader.setUniformMatrix("MMatrix", model);

				transShader.setUniformf("a_color", Resources.getInstance().targetColor[0],  Resources.getInstance().targetColor[1],  Resources.getInstance().targetColor[2], Resources.getInstance().targetColor[3] + renderObjects.get(i).collideAnimation);
				targetModel.render(transShader, GL20.GL_TRIANGLES);
				
				//render hull			
				transShader.setUniformf("a_color",  Resources.getInstance().targetEdgeColor[0], Resources.getInstance().targetEdgeColor[1], Resources.getInstance().targetEdgeColor[2], Resources.getInstance().targetEdgeColor[3] + renderObjects.get(i).collideAnimation);
				targetModel.render(transShader, GL20.GL_LINE_STRIP);
			}
				
		}
		
		
		transShader.end();
	}

	@Override
	public void hide() {
	}

	@Override
	public void dispose() {
		frameBuffer.dispose();
		frameBufferVert.dispose();
	}

	@Override
	public boolean keyDown(int keycode) {
		if (Gdx.input.isTouched())
			return false;
		
		if(keycode == Input.Keys.BACK) {
			exit = true;
		}

		if (keycode == Input.Keys.ESCAPE) {
			exit  = true;
		}
		
		if (keycode == Input.Keys.F) {
			if(Gdx.app.getType() == ApplicationType.Desktop) {
				if(!Gdx.graphics.isFullscreen()) {
					Gdx.graphics.setDisplayMode(Gdx.graphics.getDesktopDisplayMode().width, Gdx.graphics.getDesktopDisplayMode().height, true);		
				} else {
					Gdx.graphics.setDisplayMode(800,480, false);		
				}
				Resources.getInstance().prefs.putBoolean("fullscreen", !Resources.getInstance().prefs.getBoolean("fullscreen"));
				Resources.getInstance().fullscreenOnOff = !Resources.getInstance().prefs.getBoolean("fullscreen");
				Resources.getInstance().prefs.flush();
			}
		}
		
		if (keycode == Input.Keys.ENTER && selectedMenuItem != -1) {
			finished = true;
		}
		
		if (keycode == Input.Keys.MENU) {
			selectedMenuItem = 5;
			finished = true;
		}

		if (keycode == Input.Keys.DOWN) {
			selectedMenuItem++;
			selectedMenuItem %= 6;
		}

		if (keycode == Input.Keys.UP) {
			if (selectedMenuItem > 0)
				selectedMenuItem--;
			else
				selectedMenuItem = 3;
		}
		
		if(Resources.getInstance().debugMode) {	
			if(keycode == Input.Keys.H) {
				try {
					ScreenshotSaver.saveScreenshot("screenshot");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}	
			
			if(keycode == Input.Keys.O) {
				game.setScreen(new QBLogo(game));
			}	
		}
		
		return false;
	}

	@Override
	public boolean keyUp(int keycode) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean keyTyped(char character) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean touchDown(int x, int y, int pointer, int button) {
		x = (int) (x / (float) Gdx.graphics.getWidth() * 800);
		y = (int) (y / (float) Gdx.graphics.getHeight() * 480);
		
		if(!finished) {
			if (button1.contains(new Vector3(x, y, 0))) {
				selectedMenuItem = 0;
				finished = true;
			} else if (button2.contains(new Vector3(x, y, 0))) {
				selectedMenuItem = 1;
				finished = true;
			} else if (button3.contains(new Vector3(x, y, 0))) {
				selectedMenuItem = 2;
				finished = true;
			} else if (button4.contains(new Vector3(x, y, 0))) {
				selectedMenuItem = 3;
				finished = true;
			} else if (button5.contains(new Vector3(x, y, 0))) {
				selectedMenuItem = 4;
				finished = true;
			} else if (button6.contains(new Vector3(x, y, 0))) {
				selectedMenuItem = 5;
				finished = true;
			}  else {
				selectedMenuItem = -1;
			}
		}

		return false;
	}

	@Override
	public boolean touchUp(int x, int y, int pointer, int button) {
		x = (int) (x / (float) Gdx.graphics.getWidth() * 800);
		y = (int) (y / (float) Gdx.graphics.getHeight() * 480);

		return false;
	}

	@Override
	public boolean touchDragged(int x, int y, int pointer) {
		x = (int) (x / (float) Gdx.graphics.getWidth() * 800);
		y = (int) (y / (float) Gdx.graphics.getHeight() * 480);

		return false;
	}

	@Override
	public boolean mouseMoved(int x, int y) {
		x = (int) (x / (float) Gdx.graphics.getWidth() * 800);
		y = (int) (y / (float) Gdx.graphics.getHeight() * 480);
		
		if(!finished) {
			if (button1.contains(new Vector3(x, y, 0))) {
				selectedMenuItem = 0;
			} else if (button2.contains(new Vector3(x, y, 0))) {
				selectedMenuItem = 1;
			} else if (button3.contains(new Vector3(x, y, 0))) {
				selectedMenuItem = 2;
			} else if (button4.contains(new Vector3(x, y, 0))) {
				selectedMenuItem = 3;
			} else if (button5.contains(new Vector3(x, y, 0))) {
				selectedMenuItem = 4;
			} else if (button6.contains(new Vector3(x, y, 0))) {
				selectedMenuItem = 5;
			} else {
				selectedMenuItem = -1;
			}
		}
		return false;
	}

	@Override
	public boolean scrolled(int amount) {
		return false;
	}

}