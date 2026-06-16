//
// Created by emile on 18/04/2024.
//

#ifdef __arm__

long int lrand48(void);

//Before API 21 arm 32 does not expoert rand(), VPX library for GZDoom is built against API 21+ so need to define this here
int rand(void)
{
    return (int)lrand48();
}
#endif