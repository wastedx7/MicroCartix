#include <stdio.h>

void* my_memcpy(const void* src, void* dest, size_t n){
    const char* s = (const char*) src;
    char* d = (char*) dest;
    for(size_t i=0; i<n; i++){
        d[i] = s[i];
    }
    return dest;
}

int main(){
    return 0;
}