package com.watabou.noosa.ui;
//Stands in for a host's Component-based text widget (SPD's RenderedTextBlock).
//It exists to prove the font seam is shape-agnostic: a plain Visual cannot represent
//this, so a seam typed as Visual would make it impossible for a host to render the
//glyphs its language needs.
public class BlockText extends Component {
    private String text;
    public BlockText(String text) { super(); this.text = text; setSize(8, 6); }
    public String text() { return text; }
    public void hardlight(int c) {}
}
