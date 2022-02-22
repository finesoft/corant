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

import static org.corant.shared.util.Empties.isNotEmpty;
import java.awt.Frame;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JFrame;
import javax.swing.JLabel;
import org.corant.kernel.logging.LoggerFactory;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import com.carrotsearch.hppc.FloatArrayList;
import com.carrotsearch.hppc.IntArrayList;
import nu.pattern.OpenCV;

/**
 * corant-modules-aicv-yolo
 *
 * @author bingo 下午12:04:28
 *
 */
public class Test {
  static {
    LoggerFactory.disableAccessWarnings();
  }

  public static void main(String[] args) throws InterruptedException {
    OpenCV.loadShared();
    String modelWeights = "E:/AiModelRepo/yolov3.weights";
    String modelConfiguration = "E:/AiModelRepo/yolov3.cfg";
    String filePath = "D:\\VID_20211217_210759.mp4";

    AtomicBoolean running = new AtomicBoolean(true);
    JFrame jframe = new JFrame("Video");
    JLabel vidpanel = new JLabel();
    jframe.setContentPane(vidpanel);
    jframe.setVisible(true);
    jframe.setExtendedState(Frame.MAXIMIZED_BOTH);
    jframe.addWindowListener(new java.awt.event.WindowAdapter() {
      @Override
      public void windowClosing(java.awt.event.WindowEvent e) {
        e.getWindow().dispose();
        running.set(false);
      }
    });
    Net net = Dnn.readNetFromDarknet(modelConfiguration, modelWeights);
    net.setPreferableBackend(3);
    net.setPreferableTarget(0);
    Size sz = new Size(416, 416);
    List<Mat> result = new ArrayList<>();
    List<String> outBlobNames = Nets.getOutputNames(net);

    VideoCapture cap = new VideoCapture(filePath);
    Mat frame = new Mat();
    begin: while (running.get()) {
      if (running.get() && cap.read(frame)) {
        Mat blob = Dnn.blobFromImage(frame, 1 / 255.0, sz, new Scalar(0), true, false);
        net.setInput(blob);
        for (String name : outBlobNames) {
          if (!running.get()) {
            break begin;
          }
          net.forward(result, name);
          float confThreshold = 0.6f;
          IntArrayList clsIds = new IntArrayList();
          FloatArrayList confs = new FloatArrayList();
          List<Rect> rects = new ArrayList<>();
          for (int i = 0; i < result.size(); ++i) {
            Mat level = result.get(i);
            for (int j = 0; j < level.rows(); ++j) {
              if (!running.get()) {
                break begin;
              }
              Mat row = level.row(j);
              Mat scores = row.colRange(5, level.cols());
              Core.MinMaxLocResult mm = Core.minMaxLoc(scores);
              float confidence = (float) mm.maxVal;
              Point classIdPoint = mm.maxLoc;
              if (confidence > confThreshold) {
                int centerX = (int) (row.get(0, 0)[0] * frame.cols());
                int centerY = (int) (row.get(0, 1)[0] * frame.rows());
                int width = (int) (row.get(0, 2)[0] * frame.cols());
                int height = (int) (row.get(0, 3)[0] * frame.rows());
                int left = centerX - width / 2;
                int top = centerY - height / 2;
                clsIds.add((int) classIdPoint.x);
                confs.add(confidence);
                rects.add(new Rect(left, top, width, height));
              }
            }
          }
          if (isNotEmpty(confs)) {
            float nmsThresh = 0.5f;
            MatOfFloat confidences = new MatOfFloat(Mats.from(confs));
            Rect[] boxesArray = rects.toArray(new Rect[0]);
            MatOfRect boxes = new MatOfRect(boxesArray);
            MatOfInt indices = new MatOfInt();
            Dnn.NMSBoxes(boxes, confidences, confThreshold, nmsThresh, indices);
            int[] ind = indices.toArray();
            for (int idx : ind) {
              Rect box = boxesArray[idx];
              Imgproc.rectangle(frame, box.tl(), box.br(), new Scalar(0, 0, 255), 2);
            }
          }
        }
        if (vidpanel != null && running.get()) {
          try {
            vidpanel.getGraphics().drawImage(Mats.toBufferedImage(frame, null), 0, 0,
                vidpanel.getWidth(), vidpanel.getHeight(), null);
          } catch (Exception e) {

          }
        }
      }
    }
  }

}
