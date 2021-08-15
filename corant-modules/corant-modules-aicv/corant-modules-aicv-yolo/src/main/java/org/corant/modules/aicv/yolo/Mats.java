/*
 * Copyright (c) 2013-2021, Bingo.Chen (finesoft@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.corant.modules.aicv.yolo;

import static org.corant.shared.util.Empties.sizeOf;
import static org.corant.shared.util.Strings.defaultString;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import javax.imageio.ImageIO;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import com.carrotsearch.hppc.FloatArrayList;

/**
 * corant-modules-aicv-yolo
 *
 * @author bingo 下午2:25:44
 *
 */
public class Mats {

  public static Mat from(float[] floats) {
    Mat res;
    int count = sizeOf(floats);
    if (count > 0) {
      res = new Mat(count, 1, CvType.CV_32FC1);
      float[] buff = Arrays.copyOf(floats, count);
      res.put(0, 0, buff);
    } else {
      res = new Mat();
    }
    return res;
  }

  public static Mat from(FloatArrayList floats) {
    Mat res;
    int count = floats == null ? 0 : floats.size();
    if (count > 0) {
      res = new Mat(count, 1, CvType.CV_32FC1);
      float[] buff = new float[count];
      for (int i = 0; i < count; i++) {
        float f = floats.get(i);
        buff[i] = f;
      }
      res.put(0, 0, buff);
    } else {
      res = new Mat();
    }
    return res;
  }

  public static Mat from(List<Float> floats) {
    Mat res;
    int count = sizeOf(floats);
    if (count > 0) {
      res = new Mat(count, 1, CvType.CV_32FC1);
      float[] buff = new float[count];
      for (int i = 0; i < count; i++) {
        float f = floats.get(i);
        buff[i] = f;
      }
      res.put(0, 0, buff);
    } else {
      res = new Mat();
    }
    return res;
  }

  public static BufferedImage toBufferedImage(Mat image, String ext) {
    MatOfByte bytemat = new MatOfByte();
    Imgcodecs.imencode(defaultString(ext, ".jpg"), image, bytemat);
    byte[] bytes = bytemat.toArray();
    InputStream in = new ByteArrayInputStream(bytes);
    BufferedImage img = null;
    try {
      img = ImageIO.read(in);
    } catch (IOException e) {
    }
    return img;
  }
}
