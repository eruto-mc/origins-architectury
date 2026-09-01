package io.github.apace100.origins.screen;

/**
 * eruto patch: 「いま開いている選択画面はやめられるか」の印と、やめると伝える口。
 *
 * <p>⚠⚠ <b>なぜ origins の側に置くか（2026-09-01・依頼者の意向）</b>:
 * この機能はもともと {@code shiftingorigins} の mixin が
 * {@code ChooseOriginScreen} を<b>外から書き換えて</b>実現していた。
 * ⚠ 1つの画面に実装が2つある状態で、⚠⚠ <b>どちらが何をしているかは
 * 両方を開かないと分からない</b>——依頼者の言う「複雑な CSS 構造みたいな修正の難しさ」。
 *
 * <p>⚠ そこで<b>依存の向きを逆にした</b>。以前は
 * {@code origins ← shiftingorigins}（mixin で書き換える）だったが、
 * ⚠ 素直に畳もうとすると {@code CancelClient → Net → shiftingorigins} で
 * ⚠⚠ <b>循環参照になって組み立てられない</b>。
 *
 * <p>いまは <b>origins が印と口だけを持ち、
 * {@code shiftingorigins} が起動時に「やめると伝える中身」を登録する</b>。
 * ⚠ 依存は片道（{@code shiftingorigins → origins}）で、循環しない。
 *
 * <p>⚠ <b>既定で立てないのが肝。</b> {@code /origin gui} など珠以外で開いた画面まで
 * 閉じられると、層が空のまま残って {@code hasAllOrigins()} が偽＝
 * ⚠⚠ <b>無敵の状態で詰む</b>。立てるのはサーバー（珠を使った直後）だけ。
 *
 * <p>⚠ クライアントでしか触らない。サーバー側から呼ばないこと。
 */
public final class OriginSelectionCancel {

	/** ⚠ 既定は false（やめられない）。上の「無敵で詰む」を参照。 */
	private static boolean cancelable;

	/**
	 * やめると伝える中身。⚠ <b>登録されていなければ何もしない</b>——
	 * ⚠⚠ {@code shiftingorigins} を入れていない構成でも画面が壊れないようにする。
	 */
	private static Runnable sender = () -> {
	};

	private OriginSelectionCancel() {
	}

	/** eruto patch: やめると伝える中身を登録する（{@code shiftingorigins} が起動時に呼ぶ）。 */
	public static void setSender(Runnable value) {
		sender = (value != null) ? value : () -> {
		};
	}

	public static void set(boolean value) {
		cancelable = value;
	}

	public static boolean isCancelable() {
		return cancelable;
	}

	/** やめると伝えて、印を下ろす。 */
	public static void requestCancel() {
		cancelable = false;
		sender.run();
	}

	/**
	 * 伝えずに印だけ下ろす。
	 *
	 * <p>⚠ 使う場面は2つ: <b>選び終えた</b>ときと、<b>接続が切れた</b>とき。
	 * ⚠⚠ 接続が切れたのに残したままだと、入り直したあとに {@code /origin gui} で
	 * 開いた画面まで Esc で閉じられる。⚠ そのときサーバーには控えが無い（退場時に捨てている）ので
	 * <b>層が空のまま閉じる</b>＝無敵で詰む。
	 */
	public static void clear() {
		cancelable = false;
	}
}
