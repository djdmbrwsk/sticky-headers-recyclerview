package com.timehop.stickyheadersrecyclerview.caching;

import android.support.v4.util.LongSparseArray;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersAdapter;
import com.timehop.stickyheadersrecyclerview.util.OrientationProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * An implementation of {@link HeaderProvider} that creates and caches header views
 */
public class HeaderViewCache implements HeaderProvider {

  private final StickyRecyclerHeadersAdapter mAdapter;
  private final LongSparseArray<RecyclerView.ViewHolder> mHeaderViewHolders = new LongSparseArray<>();
  private final Stack<RecyclerView.ViewHolder> mScrappedHeaderViewHolders = new Stack<>();
  private final OrientationProvider mOrientationProvider;

  public HeaderViewCache(StickyRecyclerHeadersAdapter adapter,
      OrientationProvider orientationProvider) {
    mAdapter = adapter;
    mOrientationProvider = orientationProvider;
  }

  @Override
  public View getHeader(RecyclerView parent, int position) {
    long headerId = mAdapter.getHeaderId(position);

    RecyclerView.ViewHolder headerViewHolder = mHeaderViewHolders.get(headerId);
    if (headerViewHolder == null) {

      // Use scrapped view holder/view if available (recycle)
      if (mScrappedHeaderViewHolders.size() > 0) {
        headerViewHolder = mScrappedHeaderViewHolders.pop();
      } else {
        headerViewHolder = mAdapter.onCreateHeaderViewHolder(parent);
      }

      mAdapter.onBindHeaderViewHolder(headerViewHolder, position);
      View header = headerViewHolder.itemView;
      if (header.getLayoutParams() == null) {
        header.setLayoutParams(new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
      }

      int widthSpec;
      int heightSpec;

      if (mOrientationProvider.getOrientation(parent) == LinearLayoutManager.VERTICAL) {
        widthSpec = View.MeasureSpec.makeMeasureSpec(parent.getWidth(), View.MeasureSpec.EXACTLY);
        heightSpec = View.MeasureSpec.makeMeasureSpec(parent.getHeight(), View.MeasureSpec.UNSPECIFIED);
      } else {
        widthSpec = View.MeasureSpec.makeMeasureSpec(parent.getWidth(), View.MeasureSpec.UNSPECIFIED);
        heightSpec = View.MeasureSpec.makeMeasureSpec(parent.getHeight(), View.MeasureSpec.EXACTLY);
      }

      int childWidth = ViewGroup.getChildMeasureSpec(widthSpec,
          parent.getPaddingLeft() + parent.getPaddingRight(), header.getLayoutParams().width);
      int childHeight = ViewGroup.getChildMeasureSpec(heightSpec,
          parent.getPaddingTop() + parent.getPaddingBottom(), header.getLayoutParams().height);
      header.measure(childWidth, childHeight);
      header.layout(0, 0, header.getMeasuredWidth(), header.getMeasuredHeight());
      mHeaderViewHolders.put(headerId, headerViewHolder);
    }
    return headerViewHolder.itemView;
  }

  @Override
  public void recycleHeaders(List<Integer> visiblePositions) {
    // Get visible headers
    List<Long> visibleHeaderIds = new ArrayList<Long>();
    for (int i = 0; i < visiblePositions.size(); i++) {
      long headerId = mAdapter.getHeaderId(visiblePositions.get(i));
      if (!visibleHeaderIds.contains(headerId)) {
        visibleHeaderIds.add(headerId);
      }
    }

    // Scrap un-used view holders
    for (int i = 0; i < mHeaderViewHolders.size(); i++) {
      if (!visibleHeaderIds.contains(mHeaderViewHolders.keyAt(i))) {

        mScrappedHeaderViewHolders.push(mHeaderViewHolders.valueAt(i));
        mHeaderViewHolders.removeAt(i);

      }
    }
  }

  @Override
  public void invalidate() {
    mHeaderViewHolders.clear();
    mScrappedHeaderViewHolders.clear();
  }
}
