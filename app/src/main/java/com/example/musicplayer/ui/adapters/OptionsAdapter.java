package com.example.musicplayer.ui.adapters;

import android.view.View;

import com.example.musicplayer.library.LibraryObject;

import java.util.Collection;
import java.util.List;

public abstract class OptionsAdapter<T extends LibraryObject, VH extends OptionsAdapter.OptionsHolder> extends BaseAdapter<VH> {

	public enum DataType {
		Mutable, Immutable
	}

	public interface OnLongClickListener {
		void onItemLongClick(View v, int position, long id);
	}

	public interface OnOptionsClickListener {
		void onItemOptionsClick(View v, Object item);
	}

	public abstract class OptionsHolder extends BaseAdapter.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
		public OptionsHolder(View itemView) {
			super(itemView);
		}

		public abstract View getOptionsView();

		@Override
		protected void initialize() {
			super.initialize();

			View itemView = getItemView();
			if (itemView != null) {
				itemView.setOnLongClickListener(this);
			}

			View optionsView = getOptionsView();
			if (optionsView != null)
				optionsView.setOnClickListener(this);
		}

		@Override
		public void onClick(View v) {
			super.onClick(v);

			if (v == getOptionsView() && mOptionsListener != null)
				mOptionsListener.onItemOptionsClick(v, getItem(getAdapterPosition()));
		}

		@Override
		public boolean onLongClick(View v) {
			if (v == getItemView()) {
				if (mLongClickListener != null)
					mLongClickListener.onItemLongClick(v, getAdapterPosition(), getId(getAdapterPosition()));
				else if (mOptionsListener != null)
					mOptionsListener.onItemOptionsClick(v, getItem(getAdapterPosition()));
				return true;
			}

			return false;
		}
	}

	private LibraryObject[] mItems;
	private List<T> mItemList;
	private int mItemsCount;
	private DataType mDataType;

	private OnLongClickListener mLongClickListener;
	private OptionsAdapter.OnOptionsClickListener mOptionsListener;

	protected OptionsAdapter() {
		mItems = null;
		mItemList = null;
		mItemsCount = 0;
		mDataType = DataType.Immutable;
	}

	protected OptionsAdapter(T[] items, int itemsCount) {
		setItems(items, itemsCount);
	}

	protected OptionsAdapter(Collection<T> items) {
		setItems(items);
	}

	protected OptionsAdapter(List<T> items) {
		setItems(items);
	}

	@Override
	public int getItemCount() {
		switch (mDataType) {
			case Mutable:
				if (mItemList != null)
					return mItemList.size();
				break;
			case Immutable:
				return mItemsCount;
		}

		return 0;
	}

	public T getItem(int position) {
		return getActualItem(position);
	}

	public T getActualItem(int position) {
		switch (mDataType) {
			case Mutable:
				return mItemList.get(position);
			case Immutable: {
				@SuppressWarnings("unchecked")
				T item = (T)mItems[position];
				return item;
			}
		}

		return null;
	}

	@Override
	public int getId(int position) {
		return getItem(position).getId();
	}

	public T[] getItems() {
		if (mDataType == DataType.Mutable)
			return null;

		@SuppressWarnings("unchecked")
		T[] items = (T[])mItems;
		return items;
	}

	public List<T> getItemList() {
		return mItemList;
	}

	public void setItems(T[] items, int itemsCount) {
		mDataType = DataType.Immutable;
		mItemsCount = itemsCount;
		mItemList = null;
		mItems = items;
	}

	public void setItems(Collection<T> items) {
		mDataType = DataType.Immutable;
		mItemsCount = items.size();
		mItemList = null;

		if (mItems == null || mItems.length < mItemsCount)
			mItems = new LibraryObject[mItemsCount];

		items.toArray(mItems);
	}

	public void setItems(List<T> items) {
		mDataType = DataType.Mutable;
		mItemsCount = items.size();
		mItemList = items;
	}

	public void setOnLongClickListener(OnLongClickListener l) {
		mLongClickListener = l;
	}

	public void setOnOptionsClickListener(OnOptionsClickListener l) {
		mOptionsListener = l;
	}
}
