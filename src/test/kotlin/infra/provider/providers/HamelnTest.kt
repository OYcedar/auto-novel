package infra.provider.providers

import infra.model.WebNovelAttention
import infra.model.WebNovelType
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import kotlinx.datetime.Instant

class HamelnTest : DescribeSpec({
    val provider = Hameln()

    describe("getMetadata") {
        it("常规") {
            // https://syosetu.org/novel/232822
            val metadata = provider.getMetadata("232822")
            metadata.title.shouldBe("和風ファンタジーな鬱エロゲーの名無し戦闘員に転生したんだが周囲の女がヤベー奴ばかりで嫌な予感しかしない件")
            metadata.introduction.shouldStartWith("どうやら和風ファンタジーゲ")
            metadata.type.shouldBe(WebNovelType.连载中)
            metadata.attentions.shouldContain(WebNovelAttention.R15)
            metadata.keywords.shouldContain("妖")
            metadata.toc[0].title.shouldBe("人物紹介・短編等")
            metadata.toc[0].chapterId.shouldBeNull()
            metadata.toc[0].createAt.shouldBeNull()
            metadata.toc[1].title.shouldBe("人物紹介(仮設)")
            metadata.toc[1].chapterId.shouldBe("1")
            metadata.toc[1].createAt.shouldBe(Instant.parse("2021-06-19T23:00:00Z"))
        }
        it("常规，作者无链接") {
            // https://syosetu.org/novel/305149
            val author = provider.getMetadata("305149").authors.first()
            author.name.shouldBe("文章修行僧")
            author.link.shouldBeNull()
        }
        it("常规，作者有链接") {
            // https://syosetu.org/novel/304380
            val author = provider.getMetadata("304380").authors.first()
            author.name.shouldBe("かりん2022")
            author.link.shouldBe("https://syosetu.org/user/347335/")
        }
        it("常规，R18重定向") {
            // https://syosetu.org/novel/94938
            val metadata = provider.getMetadata("94938")
            metadata.title.shouldBe("オズの国のドロシー")
        }
        it("短篇") {
            // https://syosetu.org/novel/303189
            val author = provider.getMetadata("303189").authors.first()
            author.name.shouldBe("皆でワンピ章ボス全員憑依目指そう")
            author.link.shouldBeNull()
        }
    }

    describe("getEpisode") {
        it("常规") {
            // https://syosetu.org/novel/232822/1.html
            val episode = provider.getChapter("232822", "1")
            episode.paragraphs.size.shouldBe(671)
            episode.paragraphs.first().shouldBe("伴部(ともべ)")
        }
        it("短篇") {
            // https://syosetu.org/novel/303596
            val episode = provider.getChapter("303596", "default")
            episode.paragraphs.size.shouldBe(141)
            episode.paragraphs.first().shouldStartWith("　特級呪霊花御による、呪術高専東京校への襲撃")
        }
    }
})