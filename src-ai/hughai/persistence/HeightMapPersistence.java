// Copyright Hugh Perkins 2006, 2009
// hughperkins@gmail.com http://manageddreams.com
//
// This program is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License as published by the
// Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
// or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
//  more details.
//
// You should have received a copy of the GNU General Public License along
// with this program in the file licence.txt; if not, write to the
// Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-
// 1307 USA
// You can find the licence also on the web at:
// http://www.opensource.org/licenses/gpl-license.php
//

package hughai.persistence;

import java.awt.*;
import java.awt.image.*;
import java.util.*;
import java.io.*;
import javax.imageio.ImageIO;

import com.springrts.ai.*;
import com.springrts.ai.oo.*;

import hughai.CSAI;
import hughai.PlayerObjects;
import hughai.VoiceCommandHandler;
import hughai.basictypes.*;
import hughai.*;
import hughai.mapping.*;
import hughai.utils.*;



public class HeightMapPersistence
{
	public String filename = "heightmap.bmp";

	LogFile logfile;
	CSAI CSAI;
//	HeightMap heightMap;
	Maps maps;

	HeightMapPersistence( PlayerObjects playerObjects )
	{
		logfile = playerObjects.getLogFile();
		CSAI = playerObjects.getCSAI();
//		heightMap = playerObjects.getHeightMap();
		maps = playerObjects.getMaps();

		logfile.WriteLine("HeightMapPersistence");
		VoiceCommandHandler handler = new SaveHandler();
		logfile.WriteLine("csai == null? " + (CSAI == null));
		logfile.WriteLine("handler == null? " + (handler == null));
		CSAI.RegisterVoiceCommand("saveheightmap", handler);
	}
/*
	public void Load()
	{
		BufferedImage bufferedImage = ImageIO.read(new File( filename ) );
//            Bitmap bitmap = Bitmap.FromFile(filename) as Bitmap;
            int width = bufferedImage.getWidth();
            int height = bufferedImage.getHeight();
            heightMap.Width = width;
            heightMap.Height = height;
            heightMap.Map = new float[width][ height];
            double minheight = hughai.config.GetInstance().minheight;
            double maxheight = hughai.config.GetInstance().maxheight;
            double heightmultiplier = (maxheight - minheight) / 255;
            for (int i = 0; i < width; i++)
            {
                for (int j = 0; j < height; j++)
                {
                    HeightMap.GetInstance().Map[i, j] = (float)(minheight + heightmultiplier * bitmap.GetPixel(i, j).B);
                }
            }
		 
	}
*/
	void Save( String filename, float[][]mesh )
	{
//		throw new RuntimeException("Unimplemented");
		
		int width = mesh.length;
		int height = mesh[0].length;
		BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics g = bufferedImage.getGraphics();		
		for (int i = 0; i < width; i++)
		{
			for (int j = 0; j < height; j++)
			{
				int color = (int)(mesh[i][j] * 255);
				g.setColor(new Color(color,color,color));
				g.drawRect(i, j, 1, 1);
			}
		}
		try{
   		   ImageIO.write(bufferedImage, "png", new File( filename ) );
		} catch( Exception e ) {
			throw new RuntimeException( e );
		}
	}

	public class SaveHandler implements VoiceCommandHandler {
		@Override
		public void commandReceived(String cmd, String[]args, int player)
		{
//			float[][]mesh = maps.getHeightMap().GetHeightMap();
//			Save(args[2], mesh);
		}
	}
}

