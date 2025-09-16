# MaidUseHandCrank

Allows Touhou Little Maid to use Create's Hand Crank

<p align="center">
    <a href="README.md">简体中文</a> | 
    <a href="README.en_us.md">English</a>
</p>

## Summary

This mod is an addon for Touhou Little Maid/Create. Simply adds the functionality for maids to operate hand cranks.

## Usage

After adding this mod, a hand crank task will appear in the maid's task menu.

![Hand Crank Image](https://s2.loli.net/2025/09/13/jtRoi6OU2cumlfG.png)

When enabled, the maid automatically searches for hand cranks to operate. The search radius defaults to 0 (auto-detect). You can set a custom radius, but the maid may repeatedly attempt to reach unreachable cranks, causing her to keep wandering back and forth.

![⑨ Using Hand Crank Image](https://s2.loli.net/2025/09/13/IJG8MVOjoeByRca.png)

The Maid will still continue to follow the player and walk around randomly as she works, and may be interrupted by going beyond the interaction distance (4 blocks, configurable) as a result, but as long as there is a crank within range, the Maid will find one later and continue working.

Random walking can be turned off in the settings, but the maid will still stop working because of following the player or for other reasons (such as exceeding the working time).

Making them sit down while they work (cushions work too) can force them to keep working.

![Settings Interface](https://s2.loli.net/2025/09/14/eriKjEh8amLD9lp.png)

## Supplementary

"smarter maid" is turned on by default, which allows the maid to target cranks in advance to prevent scrambling.

"random walk" is on by default, when the maid will not trigger bubbles (unless sitting)

## Thanks

- Touhou Little Maid: TartaricAcid and others
- Create Mod: Simibuli and others  
- MaidAddition: Cirmuller
  - Inspiration source (x)
  - Referenced the message bubble content from this mod

## License
- Code: [MIT](https://mit-license.org/)
- Assets: [CC BY-NC-SA 4.0](https://creativecommons.org/licenses/by-nc-sa/4.0/)
