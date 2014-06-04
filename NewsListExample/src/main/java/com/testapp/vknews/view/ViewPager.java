package com.testapp.vknews.view;


import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;

public class ViewPager extends android.support.v4.view.ViewPager {

    private final int ALLOW_CLICK_MAX_DISTANCE = 30;

    private float mFirstX = -1;
    private float mFirstY = -1;
    private boolean mShouldSkipClick = false;
		
	public abstract interface PreviewPagerListener {
		boolean canScrollPage();
		void onViewItemClick(int item);
	}

	public ViewPager(Context context) {
		super(context);
	}

	public ViewPager(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent event) {
        if (this.isEnabled()) {
            try {
                return super.onInterceptTouchEvent(event);
            } catch (Exception e) {
            	e.printStackTrace();
            }
        }        
        return false;
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (this.isEnabled()) {
            try {
                boolean b = super.onTouchEvent(event);

                int action = event.getAction();

                switch ( action & MotionEvent.ACTION_MASK ) {
                    case MotionEvent.ACTION_UP:
                            mFirstX = -1;
                            mFirstY = -1;

//            			if (! m_scrollActivated) {
//            				if (m_listener != null) {
//            					m_listener.onViewItemClick(getCurrentItem());
//            				}
//            			}
                        break;
                    case MotionEvent.ACTION_DOWN:
                        getParent().requestDisallowInterceptTouchEvent(true);
                        if (event.getPointerCount() == 1) {
                            mFirstX = event.getX();
                            mFirstY = event.getY();
                        } else {
                            mFirstX = -1;
                            mFirstY = -1;
                        }
                        break;
                    case MotionEvent.ACTION_CANCEL:
                        mFirstX = -1;
                        mFirstY = -1;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (mFirstX > 0 && mFirstY > 0) {
                            float dy = Math.abs(mFirstY - event.getY());
                            if (dy > ALLOW_CLICK_MAX_DISTANCE) {
                                getParent().requestDisallowInterceptTouchEvent(false);
                                b = false;
                            }
                        }
                        break;
                    default:
                        break;
                }

                return b;
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }
	
	class YScrollDetector extends SimpleOnGestureListener {
//		@Override
//		public boolean onDown (MotionEvent e) {
//			return true;
//		}
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {        	
            if(Math.abs(distanceY) > Math.abs(distanceX)) {
                return true;
            }
            return false;
        }
    }
}
