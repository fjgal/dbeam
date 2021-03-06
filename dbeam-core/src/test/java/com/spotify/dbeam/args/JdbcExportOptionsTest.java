/*-
 * -\-\-
 * DBeam Core
 * --
 * Copyright (C) 2016 - 2019 Spotify AB
 * --
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -/-/-
 */

package com.spotify.dbeam.args;

import com.spotify.dbeam.options.JdbcExportArgsFactory;
import com.spotify.dbeam.options.JdbcExportPipelineOptions;

import java.io.IOException;
import java.util.Optional;

import org.apache.avro.file.CodecFactory;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.junit.Assert;
import org.junit.Test;


public class JdbcExportOptionsTest {

  JdbcExportArgs optionsFromArgs(String cmdLineArgs) throws IOException, ClassNotFoundException {
    PipelineOptionsFactory.register(JdbcExportPipelineOptions.class);
    PipelineOptions opts =
        PipelineOptionsFactory.fromArgs(cmdLineArgs.split(" ")).withValidation().create();
    return JdbcExportArgsFactory.fromPipelineOptions(opts);
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldFailParseOnInvalidArg() throws IOException, ClassNotFoundException {
    optionsFromArgs("--foo=bar");
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldFailOnMissingConnectionUrl() throws IOException, ClassNotFoundException {
    optionsFromArgs("--table=somtable");
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldFailOnMissingTable() throws IOException, ClassNotFoundException {
    optionsFromArgs("--connectionUrl=jdbc:postgresql://some_db");
  }

  @Test
  public void shouldParseWithDefaultsOnConnectionUrlAndTable()
      throws IOException, ClassNotFoundException {
    JdbcExportArgs actual = optionsFromArgs(
        "--connectionUrl=jdbc:postgresql://some_db --table=some_table");

    JdbcExportArgs expected = JdbcExportArgs.create(
        JdbcAvroArgs.create(
            JdbcConnectionArgs.create("jdbc:postgresql://some_db")
                .withUsername("dbeam-extractor")
        ),
        QueryBuilderArgs.create("some_table")
    );

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void shouldParseWithMySqlConnection() throws IOException, ClassNotFoundException {
    JdbcExportArgs actual = optionsFromArgs(
        "--connectionUrl=jdbc:mysql://some_db --table=some_table");

    JdbcExportArgs expected = JdbcExportArgs.create(
        JdbcAvroArgs.create(
            JdbcConnectionArgs.create("jdbc:mysql://some_db")
                .withUsername("dbeam-extractor")
        ),
        QueryBuilderArgs.create("some_table")
    );

    Assert.assertEquals(expected, actual);
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldFailOnInvalidTable() throws IOException, ClassNotFoundException {
    optionsFromArgs("--connectionUrl=jdbc:postgresql://some_db --table=some-table-with-dash");
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldFailOnNonJdbcUrl() throws IOException, ClassNotFoundException {
    optionsFromArgs("--connectionUrl=bar --table=sometable");
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldFailOnUnsupportedJdbcUrl() throws IOException, ClassNotFoundException {
    optionsFromArgs("--connectionUrl=jdbc:paradox:./foo --table=sometable");
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldFailOnMissingPartitionButPresentPartitionColumn()
      throws IOException, ClassNotFoundException {
    optionsFromArgs("--connectionUrl=jdbc:postgresql://some_db --table=sometable "
                    + "--partitionColumn=col");
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldFailOnTooOldPartition() throws IOException, ClassNotFoundException {
    optionsFromArgs("--connectionUrl=jdbc:postgresql://some_db --table=sometable "
                    + "--partition=2015-01-01");
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldFailOnTooOldPartitionWithConfiguredMinPartitionPeriodMoreThanPartition()
      throws IOException, ClassNotFoundException {
    optionsFromArgs("--connectionUrl=jdbc:postgresql://some_db --table=sometable "
                    + "--partition=2015-01-01 --minPartitionPeriod=2015-01-02");
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldFailOnTooOldPartitionWithConfiguredMinPartitionPeriodLessThanPartition()
      throws IOException, ClassNotFoundException {
    optionsFromArgs("--connectionUrl=jdbc:postgresql://some_db --table=sometable "
                    + "--partition=2015-01-01 --minPartitionPeriod=2015-01-01");
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldFailOnNonJdbcUrl2() throws IOException, ClassNotFoundException {
    optionsFromArgs("--connectionUrl=some:foo:bar --table=sometable");
  }

  @Test
  public void shouldConfigureUserAndPassword() throws IOException, ClassNotFoundException {
    JdbcExportArgs actual = optionsFromArgs(
        "--connectionUrl=jdbc:postgresql://some_db --table=some_table "
        + "--username=someuser --password=somepassword");

    JdbcExportArgs expected = JdbcExportArgs.create(
        JdbcAvroArgs.create(
            JdbcConnectionArgs.create("jdbc:postgresql://some_db")
                .withUsername("someuser")
                .withPassword("somepassword")
        ),
        QueryBuilderArgs.create("some_table")
    );

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void shouldConfigureAvroLogicalTypes() throws IOException, ClassNotFoundException {
    JdbcExportArgs options = optionsFromArgs(
        "--connectionUrl=jdbc:postgresql://some_db --table=some_table "
        + "--password=secret --useAvroLogicalTypes=true");

    Assert.assertTrue(
        options.useAvroLogicalTypes());
  }

  @Test
  public void shouldConfigureAvroDoc() throws IOException, ClassNotFoundException {
    JdbcExportArgs options = optionsFromArgs(
        "--connectionUrl=jdbc:postgresql://some_db --table=some_table "
        + "--password=secret --avroDoc=somedoc");

    Assert.assertEquals(
        Optional.of("somedoc"),
        options.avroDoc());
  }

  @Test
  public void shouldConfigureAvroSchemaNamespace() throws IOException, ClassNotFoundException {
    JdbcExportArgs options = optionsFromArgs(
        "--connectionUrl=jdbc:postgresql://some_db --table=some_table "
        + "--password=secret --avroSchemaNamespace=ns");

    Assert.assertEquals(
        "ns",
        options.avroSchemaNamespace());
  }

  @Test
  public void shouldConfigureFetchSize() throws IOException, ClassNotFoundException {
    JdbcExportArgs options = optionsFromArgs(
        "--connectionUrl=jdbc:postgresql://some_db --table=some_table "
        + "--password=secret --fetchSize=1234");

    Assert.assertEquals(
        1234,
        options.jdbcAvroOptions().fetchSize());
  }

  @Test
  public void shouldConfigureDeflateCodec() throws IOException, ClassNotFoundException {
    JdbcExportArgs options = optionsFromArgs(
        "--connectionUrl=jdbc:postgresql://some_db --table=some_table "
        + "--password=secret --avroCodec=deflate7");

    Assert.assertEquals(
        "deflate7",
        options.jdbcAvroOptions().avroCodec());
    Assert.assertEquals(
        CodecFactory.deflateCodec(7).toString(),
        options.jdbcAvroOptions().getCodecFactory().toString());
  }

  @Test
  public void shouldConfigureZstandardCodec() throws IOException, ClassNotFoundException {
    JdbcExportArgs options = optionsFromArgs(
        "--connectionUrl=jdbc:postgresql://some_db --table=some_table "
        + "--password=secret --avroCodec=zstandard9");

    Assert.assertEquals(
        "zstandard9",
        options.jdbcAvroOptions().avroCodec());
    Assert.assertEquals(
        CodecFactory.zstandardCodec(9).toString(),
        options.jdbcAvroOptions().getCodecFactory().toString());
  }

  @Test
  public void shouldConfigureSnappyCodec() throws IOException, ClassNotFoundException {
    JdbcExportArgs options = optionsFromArgs(
        "--connectionUrl=jdbc:postgresql://some_db --table=some_table "
        + "--password=secret --avroCodec=snappy");

    Assert.assertEquals(
        "snappy",
        options.jdbcAvroOptions().avroCodec());
    Assert.assertEquals(
        CodecFactory.snappyCodec().toString(),
        options.jdbcAvroOptions().getCodecFactory().toString());
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldFailOnInvalidAvroCodec() throws IOException, ClassNotFoundException {
    optionsFromArgs(
        "--connectionUrl=jdbc:postgresql://some_db --table=some_table "
        + "--password=secret --avroCodec=lzma");
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldFailOnQueryParallelismWithNoSplitColumn()
      throws IOException, ClassNotFoundException {
    optionsFromArgs("--connectionUrl=jdbc:postgresql://some_db "
                    + "--table=some_table --password=secret --queryParallelism=10");
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldFailOnSplitColumnWithNoQueryParallelism()
      throws IOException, ClassNotFoundException {
    optionsFromArgs("--connectionUrl=jdbc:postgresql://some_db "
                    + "--table=some_table --password=secret --splitColumn=id");
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldFailOnZeroQueryParallelism()
      throws IOException, ClassNotFoundException {
    optionsFromArgs("--connectionUrl=jdbc:postgresql://some_db "
                    + "--table=some_table --password=secret --queryParallelism=0 --splitColumn=id");
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldFailOnNegativeQueryParallelism()
      throws IOException, ClassNotFoundException {
    optionsFromArgs("--connectionUrl=jdbc:postgresql://some_db --table=some_table "
                    + "--password=secret --queryParallelism=-5 --splitColumn=id");
  }

}
