/*
 * Copyright (C) 2010 The Sipdroid Open Source Project
 * Copyright (C) 2007 The Android Open Source Project
 * 
 * This file is part of Sipdroid (http://www.sipdroid.org)
 * 
 * Sipdroid is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This source code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this source code; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.sipdroid.media.file;

public class AudioFileInformations {
	public boolean success = false;
	public String error;
	public long rate;
	public int channels;
	public int encoding;
	public int bitratemode;
	public int bitrate;
	public long length;
	public int framesize;
	public FileType fileType;
	// TODO: Add meta data like IDv1 and IDv2 tags
	
	public enum FileType{
		UNKNOWN,
		MP3,
		WAV,
		AAC
	}
	
	public enum mpg123_vbr 
	{
		MPG123_CBR,   
		MPG123_VBR,             
		MPG123_ABR              
	};

	public enum mpg123_mode 
	{ 
		MPG123_M_STEREO,      
		MPG123_M_JOINT,         
		MPG123_M_DUAL,          
		MPG123_M_MONO           
	};
	
	public AudioFileInformations(){
		
	}
	
	public AudioFileInformations(FileType type){
		this.fileType = type;
	}
}
