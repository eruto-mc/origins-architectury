> ## ⚠ これは改変版です（EdwinMindcraft/origins-architectury の fork・公式のものではありません）
>
> **This is a modified version of Origins (Forge port), not the official build.**
> Modified by the minecraft club (eruto). Original work: [EdwinMindcraft/origins-architectury](https://github.com/EdwinMindcraft/origins-architectury)
> (itself a port of [apace100/origins-fabric](https://github.com/apace100/origins-fabric)).
> Licensed under **MIT**, same as upstream. The original authors do not endorse this build.
>
> 上流: [EdwinMindcraft/origins-architectury](https://github.com/EdwinMindcraft/origins-architectury) ／ 枝 `eruto/world3-1.20.1`
> ／ 上流の枝 `1.20.x/forge` から分岐。**以下は上流の README です。**
> この枝は [eruto-mc/eruto-origins](https://github.com/eruto-mc/eruto-origins) の submodule として建てる（単体では建てない）。
>
> ⚠ **不具合をここの改変版で見つけても、上流へ報告しないでください。**
>
> **当部が変えたところ**（中身は `git log --author=erutobusiness`）:
>
> | 何を | なぜ |
> | - | - |
> | 当部の追加 MOD `shifting_origins`（職業と種族の能力）を、このソースの木（`net.erutobusiness.shiftingorigins`）へ受け入れた | 2 つの jar（ビルドの単位も 2 つ）に分かれていると、依存を片道しか張れず、循環参照になっていた |
> | 種族の選択画面で、層が持つ種族を id で引き直すようにした。飛ばした能力の内訳を記録に出す | 登録データが作り直されたあと、層が古い世代の種族を指したままになり、画面に能力の詳細が 1 つも出なかった |
> | 浮遊を状態効果から外し、`travel` の中で直接持ち上げるようにした | 効果時間が短く途切れていた。さらに、悪い効果を毎 tick 剥がすほかの MOD の効果（Farm & Charm の農夫の加護）が付いていると、バニラの浮遊（悪い効果）も剥がされていた |
> | 溶岩を水と同じ物理で泳げるようにし、溶岩の中の見通しをカメラの元の判定まで届かせた | 溶岩の速さを変える口は減衰を変えるだけで、速くなっていなかった。見通しはシェーダーを入れると打ち消されていた |
> | 職業の能力を足し、切れていたものを直した（鍛冶屋・商人・司書・聖職者・木こり ほか）。アンデッドの蘇りを不死のトーテムより先に働かせた | 上流の実装が前提にしているバニラの部品を別の MOD が差し替えていて、働いていない能力があった。弱い職業には能力を足した |
> | プレイヤーが配った悪い効果（毒の雲など）で倒れた相手に、配ったプレイヤーを「やった人」として記録するようにした | 経験値やプレイヤーが倒したときだけ落ちる物が出ず、死亡メッセージにも名前が出なかった |
> | 肉しか食べない種族でも飲み物を飲めるようにし、弁当箱に食べられる物だけを選ばせた | 肉食の制限がワインやお茶まで止めていた。弁当箱は食事の制限を見ずに中身を食べさせていた |

# Origins (Forge)

This is the repository for the source of the unofficial forge port of the Origins mod.

## Resources
Datapacks: Origins forge is supposed to be fully compatible with fabric datapacks, if you want more information, please
visit [the official wiki](https://origins.readthedocs.io/) written for fabric.

## Building from source

The code to build the repository has moved to [here](https://github.com/EdwinMindcraft/origins-forge).

### Disclaimer:
The project doesn't allow for the creation of a unified build yet, which is why there isn't a release
with the new bugfixes.