# 交叉编译

## 环境检查

```bash
# gcc 检查
(base) ➜  aws-iot-securetunneling-localproxy git:(main) gcc --version
Apple clang version 14.0.0 (clang-1400.0.29.202)
Target: arm64-apple-darwin21.6.0
Thread model: posix
InstalledDir: /Library/Developer/CommandLineTools/usr/bin

# cmake安装和检查
#下载的cmake-gui, 可以通过
#sudo "/Applications/CMake.app/Contents/bin/cmake-gui" --install

brew install cmake 
cmake --version
cmake version 3.26.0-rc2

CMake suite maintained and supported by Kitware (kitware.com/cmake).

# protobuf
brew install protobuf
protoc --version #检查版本
libprotoc 3.21.12
```