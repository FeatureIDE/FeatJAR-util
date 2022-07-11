/* -----------------------------------------------------------------------------
 * util - Common utilities and data structures
 * Copyright (C) 2020 Sebastian Krieter
 * 
 * This file is part of util.
 * 
 * util is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3.0 of the License,
 * or (at your option) any later version.
 * 
 * util is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with util. If not, see <https://www.gnu.org/licenses/>.
 * 
 * See <https://github.com/FeatJAR/util> for further information.
 * -----------------------------------------------------------------------------
 */
package de.featjar.util.tree.structure;

import java.util.*;
import java.util.function.*;

/**
 * Interface for a tree node.
 *
 * @author Sebastian Krieter
 */
public interface Tree<T extends Tree<T>> {

	Tree<T> cloneNode();

	default boolean equalsNode(Object other) {
		return getClass() == other.getClass();
	}

	// todo: equals as in NonTerminal

	// todo: clone as in Trees.clone?

	default boolean hasChildren() {
		return !getChildren().isEmpty();
	}

	List<? extends T> getChildren();

	void setChildren(List<? extends T> children);

	default int getNumberOfChildren() {
		return getChildren().size();
	}

	default void flatMapChildren(Function<T, List<? extends T>> mapper) {
		Objects.requireNonNull(mapper);
		final List<? extends T> oldChildren = getChildren();
		if (!oldChildren.isEmpty()) {
			final ArrayList<T> newChildren = new ArrayList<>(oldChildren.size());
			boolean modified = false;
			for (final T child : oldChildren) {
				final List<? extends T> replacement = mapper.apply(child);
				if (replacement != null) {
					newChildren.addAll(replacement);
					modified = true;
				} else {
					newChildren.add(child);
				}
			}
			if (modified) {
				setChildren(newChildren);
			}
		}
	}

	default void mapChildren(Function<T, ? extends T> mapper) {
		Objects.requireNonNull(mapper);
		final List<? extends T> oldChildren = getChildren();
		if (!oldChildren.isEmpty()) {
			final List<T> newChildren = new ArrayList<>(oldChildren.size());
			boolean modified = false;
			for (final T child : oldChildren) {
				final T replacement = mapper.apply(child);
				if ((replacement != null) && (replacement != child)) {
					newChildren.add(replacement);
					modified = true;
				} else {
					newChildren.add(child);
				}
			}
			if (modified) {
				setChildren(newChildren);
			}
		}
	}

	default Optional<T> getFirstChild() {
		if (getChildren().isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(getChildren().get(0));
	}

	default Optional<T> getLastChild() {
		if (getChildren().isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(getChildren().get(getNumberOfChildren() - 1));
	}
}