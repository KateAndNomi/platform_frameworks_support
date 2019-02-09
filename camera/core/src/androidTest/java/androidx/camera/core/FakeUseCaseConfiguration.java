/*
 * Copyright (C) 2019 The Android Open Source Project
 *
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
 */

package androidx.camera.core;

/** A fake configuration for {@link FakeUseCase}. */
public class FakeUseCaseConfiguration
    implements UseCaseConfiguration<FakeUseCase>, CameraDeviceConfiguration {

  private final Configuration config;

  private FakeUseCaseConfiguration(Configuration config) {
    this.config = config;
  }

  @Override
  public Configuration getConfiguration() {
    return config;
  }

  /** Builder for an empty Configuration */
  public static final class Builder
      implements UseCaseConfiguration.Builder<FakeUseCase, FakeUseCaseConfiguration, Builder>,
          CameraDeviceConfiguration.Builder<FakeUseCaseConfiguration, Builder> {

    private final MutableOptionsBundle optionsBundle;

    public Builder() {
      optionsBundle = MutableOptionsBundle.create();
      setTargetClass(FakeUseCase.class);
    }

    @Override
    public MutableConfiguration getMutableConfiguration() {
      return optionsBundle;
    }

    @Override
    public Builder builder() {
      return this;
    }

    @Override
    public FakeUseCaseConfiguration build() {
      return new FakeUseCaseConfiguration(OptionsBundle.from(optionsBundle));
    }
  }
}