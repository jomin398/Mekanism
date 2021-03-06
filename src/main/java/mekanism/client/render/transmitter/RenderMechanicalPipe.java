package mekanism.client.render.transmitter;

import java.util.HashMap;

import mekanism.api.MekanismConfig.client;
import mekanism.client.render.MekanismRenderer;
import mekanism.client.render.MekanismRenderer.DisplayInteger;
import mekanism.client.render.MekanismRenderer.FluidType;
import mekanism.client.render.MekanismRenderer.Model3D;
import mekanism.common.ColourRGBA;
import mekanism.common.multipart.PartMechanicalPipe;
import mekanism.common.multipart.PartSidedPipe.ConnectionType;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.VertexBuffer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fluids.Fluid;

import org.lwjgl.opengl.GL11;

public class RenderMechanicalPipe extends RenderTransmitterBase<PartMechanicalPipe>
{
	private static HashMap<Integer, HashMap<Fluid, DisplayInteger[]>> cachedLiquids = new HashMap<Integer, HashMap<Fluid, DisplayInteger[]>>();
	
	private static final int stages = 100;
	private static final double height = 0.45;
	private static final double offset = 0.015;
	
	public RenderMechanicalPipe()
	{
		super();
	}
	
	@Override
	public void renderMultipartAt(PartMechanicalPipe pipe, double x, double y, double z, float partialTick, int destroyStage)
	{
		if(client.opaqueTransmitters)
		{
			return;
		}
		
		float targetScale;
		
		if(pipe.getTransmitter().hasTransmitterNetwork())
		{
			targetScale = pipe.getTransmitter().getTransmitterNetwork().fluidScale;
		}
		else {
			targetScale = (float)pipe.buffer.getFluidAmount() / (float)pipe.buffer.getCapacity();
		}

		if(Math.abs(pipe.currentScale - targetScale) > 0.01)
		{
			pipe.currentScale = (12 * pipe.currentScale + targetScale) / 13;
		}
		else {
			pipe.currentScale = targetScale;
		}

		Fluid fluid;

		if(pipe.getTransmitter().hasTransmitterNetwork())
		{
			fluid = pipe.getTransmitter().getTransmitterNetwork().refFluid;
		}
		else {
			fluid = pipe.getBuffer() == null ? null : pipe.getBuffer().getFluid();
		}

		float scale = pipe.currentScale;

		if(scale > 0.01 && fluid != null)
		{
			push();

			MekanismRenderer.glowOn(fluid.getLuminosity());
			MekanismRenderer.colorFluid(fluid);

			bindTexture(MekanismRenderer.getBlocksTexture());
			GL11.glTranslated(x, y, z);

			boolean gas = fluid.isGaseous();

			for(EnumFacing side : EnumFacing.VALUES)
			{
				if(pipe.getConnectionType(side) == ConnectionType.NORMAL)
				{
					DisplayInteger[] displayLists = getListAndRender(side, fluid);

					if(displayLists != null)
					{
						if(!gas)
						{
							displayLists[Math.max(3, (int)((float)scale*(stages-1)))].render();
						}
						else {
							GL11.glColor4f(1F, 1F, 1F, scale);
							displayLists[stages-1].render();
						}
					}
				}
				else if(pipe.getConnectionType(side) != ConnectionType.NONE) 
				{
					GL11.glTranslated(0.5, 0.5, 0.5);
					Tessellator tessellator = Tessellator.getInstance();
					VertexBuffer worldRenderer = tessellator.getBuffer();
					
					if(renderFluidInOut(worldRenderer, side, pipe))
					{
						tessellator.draw();
					}
					
					GL11.glTranslated(-0.5, -0.5, -0.5);
				}
			}

			DisplayInteger[] displayLists = getListAndRender(null, fluid);

			if(displayLists != null)
			{
				if(!gas)
				{
					displayLists[Math.max(3, (int)((float)scale*(stages-1)))].render();
				}
				else {
					GL11.glColor4f(1F, 1F, 1F, scale);
					displayLists[stages-1].render();
				}
			}

			MekanismRenderer.glowOff();
			MekanismRenderer.resetColor();

			pop();
		}
	}
	
	private DisplayInteger[] getListAndRender(EnumFacing side, Fluid fluid)
	{
		if(fluid == null)
		{
			return null;
		}
		
		int sideOrdinal = side != null ? side.ordinal() : 6;

		if(cachedLiquids.containsKey(sideOrdinal) && cachedLiquids.get(sideOrdinal).containsKey(fluid))
		{
			return cachedLiquids.get(sideOrdinal).get(fluid);
		}

		Model3D toReturn = new Model3D();
		toReturn.baseBlock = Blocks.WATER;
		toReturn.setTexture(MekanismRenderer.getFluidTexture(fluid, FluidType.STILL));

		if(side != null)
		{
			toReturn.setSideRender(side, false);
			toReturn.setSideRender(side.getOpposite(), false);
		}

		DisplayInteger[] displays = new DisplayInteger[stages];

		if(cachedLiquids.containsKey(sideOrdinal))
		{
			cachedLiquids.get(sideOrdinal).put(fluid, displays);
		}
		else {
			HashMap<Fluid, DisplayInteger[]> map = new HashMap<Fluid, DisplayInteger[]>();
			map.put(fluid, displays);
			cachedLiquids.put(sideOrdinal, map);
		}

		for(int i = 0; i < stages; i++)
		{
			displays[i] = DisplayInteger.createAndStart();

			switch(sideOrdinal)
			{
				case 6:
				{
					toReturn.minX = 0.25 + offset;
					toReturn.minY = 0.25 + offset;
					toReturn.minZ = 0.25 + offset;

					toReturn.maxX = 0.75 - offset;
					toReturn.maxY = 0.25 + offset + ((float)i / (float)stages)*height;
					toReturn.maxZ = 0.75 - offset;
					break;
				}
				case 0:
				{
					toReturn.minX = 0.5 - (((float)i / (float)stages)*height)/2;
					toReturn.minY = 0.0;
					toReturn.minZ = 0.5 - (((float)i / (float)stages)*height)/2;

					toReturn.maxX = 0.5 + (((float)i / (float)stages)*height)/2;
					toReturn.maxY = 0.25 + offset;
					toReturn.maxZ = 0.5 + (((float)i / (float)stages)*height)/2;
					break;
				}
				case 1:
				{
					toReturn.minX = 0.5 - (((float)i / (float)stages)*height)/2;
					toReturn.minY = 0.25 - offset + ((float)i / (float)stages)*height;
					toReturn.minZ = 0.5 - (((float)i / (float)stages)*height)/2;

					toReturn.maxX = 0.5 + (((float)i / (float)stages)*height)/2;
					toReturn.maxY = 1.0;
					toReturn.maxZ = 0.5 + (((float)i / (float)stages)*height)/2;
					break;
				}
				case 2:
				{
					toReturn.minX = 0.25 + offset;
					toReturn.minY = 0.25 + offset;
					toReturn.minZ = 0.0;

					toReturn.maxX = 0.75 - offset;
					toReturn.maxY = 0.25 + offset + ((float)i / (float)stages)*height;
					toReturn.maxZ = 0.25 + offset;
					break;
				}
				case 3:
				{
					toReturn.minX = 0.25 + offset;
					toReturn.minY = 0.25 + offset;
					toReturn.minZ = 0.75 - offset;

					toReturn.maxX = 0.75 - offset;
					toReturn.maxY = 0.25 + offset + ((float)i / (float)stages)*height;
					toReturn.maxZ = 1.0;
					break;
				}
				case 4:
				{
					toReturn.minX = 0.0;
					toReturn.minY = 0.25 + offset;
					toReturn.minZ = 0.25 + offset;

					toReturn.maxX = 0.25 + offset;
					toReturn.maxY = 0.25 + offset + ((float)i / (float)stages)*height;
					toReturn.maxZ = 0.75 - offset;
					break;
				}
				case 5:
				{
					toReturn.minX = 0.75 - offset;
					toReturn.minY = 0.25 + offset;
					toReturn.minZ = 0.25 + offset;

					toReturn.maxX = 1.0;
					toReturn.maxY = 0.25 + offset + ((float)i / (float)stages)*height;
					toReturn.maxZ = 0.75 - offset;
					break;
				}
			}

			MekanismRenderer.renderObject(toReturn);
			displays[i].endList();
		}

		return displays;
	}
	
	public boolean renderFluidInOut(VertexBuffer renderer, EnumFacing side, PartMechanicalPipe pipe)
	{
		if(pipe != null && pipe.getTransmitter() != null && pipe.getTransmitter().getTransmitterNetwork() != null)
		{
			bindTexture(MekanismRenderer.getBlocksTexture());
			TextureAtlasSprite tex = MekanismRenderer.getFluidTexture(pipe.getTransmitter().getTransmitterNetwork().refFluid, FluidType.STILL);
			renderTransparency(renderer, tex, getModelForSide(pipe, side), new ColourRGBA(1.0, 1.0, 1.0, pipe.currentScale));
			
			return true;
		}
		
		return false;
	}
	
    public static void onStitch()
    {
    	cachedLiquids.clear();
    }
}
