mkdir -p ./tmp/x86

./linux/dump_syms ../app/build/intermediates/merged_native_libs/debug/out/lib/x86/libnative-lib.so > ./tmp/x86/libnative-lib.so.sym

./linux/symupload ./tmp/x86/libnative-lib.so.sym "https://fred.bugsplat.com/post/bp/symbol/breakpadsymbols.php?appName=AndroidCrasher&appVer=1.0.1"
