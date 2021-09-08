/*
 *    This file is part of the LOD Mod, licensed under the GNU GPL v3 License.
 *
 *    Copyright (C) 2020  James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.seibel.lod.handlers;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.seibel.lod.enums.FogQuality;
import com.seibel.lod.wrapper.MinecraftWrapper;

/**
 * This object is used to get variables from methods
 * where they are private. Specifically the fog setting
 * in Optifine.
 *
 * @author James Seibel
 * @version 9-7-2021
 */
public class ReflectionHandler
{
	public static final ReflectionHandler INSTANCE = new ReflectionHandler();
	private MinecraftWrapper mc = MinecraftWrapper.INSTANCE;
	
	public Field ofFogField = null;
	public Method vertexBufferUploadMethod = null;
	
	
	
	private ReflectionHandler()
	{
		setupFogField();
	}
	
	
	/**
	 * finds the Optifine fog type field
	 */
	private void setupFogField()
	{
		// get every variable from the entity renderer
		Field[] optionFields = mc.getOptions().getClass().getDeclaredFields();
		
		// try and find the ofFogType variable in gameSettings
		for (Field field : optionFields)
		{
			if (field.getName().equals("ofFogType"))
			{
				ofFogField = field;
				return;
			}
		}
		
		// we didn't find the field,
		// either optifine isn't installed, or
		// optifine changed the name of the variable
	}
	
	
	
	
	/**
	 * Get what type of fog optifine is currently set to render.
	 */
	public FogQuality getFogQuality()
	{
		if (ofFogField == null)
		{
			// either optifine isn't installed,
			// the variable name was changed, or
			// the setup method wasn't called yet.
			return FogQuality.FANCY;
		}
		
		int returnNum = 0;
		
		try
		{
			returnNum = (int) ofFogField.get(mc.getOptions());
		} catch (IllegalArgumentException | IllegalAccessException e)
		{
			e.printStackTrace();
		}
		
		switch (returnNum)
		{
		// optifine's "default" option,
		// it should never be called in this case
		case 0:
			return FogQuality.FAST;
			
			// normal options
		case 1:
			return FogQuality.FAST;
		case 2:
			return FogQuality.FANCY;
		case 3:
			return FogQuality.OFF;
			
		default:
			return FogQuality.FAST;
		}
	}
}
