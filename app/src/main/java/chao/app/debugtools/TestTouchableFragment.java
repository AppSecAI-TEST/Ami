package chao.app.debugtools;

import android.view.MotionEvent;
import android.view.View;

import chao.app.ami.Ami;
import chao.app.ami.annotations.LayoutID;
import chao.app.ami.base.AMISupportFragment;

/**
 * @author chao.qin
 * @since 2017/8/9
 */
@LayoutID(R.layout.test_touchable_fragment)
public class TestTouchableFragment extends AMISupportFragment implements View.OnClickListener, View.OnTouchListener, View.OnLongClickListener {
    @Override
    public void setupView(View layout) {
        findView(R.id.lll).setOnClickListener(this);
        findView(R.id.view).setOnTouchListener(this);
        findView(R.id.view).setOnLongClickListener(this);
    }

    @Override
    public void onClick(View v) {
        Ami.log("test onclick");
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        Ami.log("test onTouch");
        return false;
    }


    @Override
    public boolean onLongClick(View v) {
        Ami.log("test Long click!");
        return false;
    }
}
