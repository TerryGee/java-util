/*
 * Copyright 2011,2012 Metamarkets Group Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.metamx.common.io.smoosh;

import com.google.common.io.Files;
import com.google.common.primitives.Ints;
import junit.framework.Assert;
import org.junit.Test;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 */
public class SmooshedFileMapperTest
{
  @Test
  public void testSanity() throws Exception
  {
    File baseDir = Files.createTempDir();

    try {
      FileSmoosher smoosher = new FileSmoosher(baseDir, 21);
      for (int i = 0; i < 20; ++i) {
        File tmpFile = File.createTempFile(String.format("smoosh-%s", i), ".bin");
        Files.write(Ints.toByteArray(i), tmpFile);
        smoosher.add(String.format("%d", i), tmpFile);
        tmpFile.delete();
      }
      smoosher.close();

      File[] files = baseDir.listFiles();
      Arrays.sort(files);

      Assert.assertEquals(5, files.length);
      for (int i = 0; i < 4; ++i) {
        Assert.assertEquals(FileSmoosher.makeChunkFile(baseDir, i), files[i]);
      }
      Assert.assertEquals(FileSmoosher.metaFile(baseDir), files[files.length - 1]);

      SmooshedFileMapper mapper = SmooshedFileMapper.load(baseDir);
      for (int i = 0; i < 20; ++i) {
        ByteBuffer buf = mapper.mapFile(String.format("%d", i));
        Assert.assertEquals(0, buf.position());
        Assert.assertEquals(4, buf.remaining());
        Assert.assertEquals(4, buf.capacity());
        Assert.assertEquals(i, buf.getInt());
      }
      mapper.close();
    }
    finally {
      for (File file : baseDir.listFiles()) {
        file.delete();
      }
    }
  }
}
