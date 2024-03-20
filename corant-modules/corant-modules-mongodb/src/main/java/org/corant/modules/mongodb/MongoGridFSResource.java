/*
 * Copyright (c) 2013-2023, Bingo.Chen (finesoft@gmail.com).
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
package org.corant.modules.mongodb;

import static org.corant.shared.util.Assertions.shouldNotNull;
import org.corant.shared.resource.InputStreamResource;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.model.GridFSFile;

/**
 * corant-modules-mongodb
 *
 * @author bingo 15:05:55
 */
public class MongoGridFSResource extends InputStreamResource {

  protected final GridFSFile gridFSFile;

  public MongoGridFSResource(GridFSBucket bucket, GridFSFile file) {
    super(
        shouldNotNull(bucket, "GridFS bucket can't null")
            .openDownloadStream(shouldNotNull(file, "GridFS file can't null").getId()),
        bucket.getBucketName(), file.getFilename(), file.getMetadata());
    gridFSFile = file;
  }

  public GridFSFile getGridFSFile() {
    return gridFSFile;
  }

}
