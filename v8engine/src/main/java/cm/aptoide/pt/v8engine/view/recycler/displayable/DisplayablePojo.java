/*
 * Copyright (c) 2016.
 * Modified by Neurophobic Animal on 04/05/2016.
 */

package cm.aptoide.pt.v8engine.view.recycler.displayable;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Created by neuro on 14-04-2016.
 */
@Accessors(chain = true)
public abstract class DisplayablePojo<T> extends Displayable {

	//private static final float REFERENCE_WIDTH_DPI = 360;
	@Getter @Setter private T pojo;

	/**
	 * Needed for reflective {@link Class#newInstance()}.
	 */
	public DisplayablePojo() {
		this(null, false);
	}

	public DisplayablePojo(T pojo) {
		this(pojo, false);
	}

	public DisplayablePojo(T pojo, boolean fixedPerLineCount) {
		super(fixedPerLineCount);
		this.pojo = pojo;
	}
}
