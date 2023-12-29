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

import java.util.ArrayList;
import java.util.List;
import org.opencv.dnn.Net;

/**
 * corant-modules-aicv-yolo
 *
 * @author bingo 下午2:24:52
 */
public class Nets {

  public static List<String> getOutputNames(Net net) {
    List<String> names = new ArrayList<>();
    List<Integer> outLayers = net.getUnconnectedOutLayers().toList();
    List<String> layersNames = net.getLayerNames();
    outLayers.forEach(item -> names.add(layersNames.get(item - 1)));
    return names;
  }
}
