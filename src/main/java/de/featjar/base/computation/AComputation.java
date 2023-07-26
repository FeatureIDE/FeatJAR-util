/*
 * Copyright (C) 2023 Sebastian Krieter, Elias Kuiter
 *
 * This file is part of FeatJAR-base.
 *
 * base is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3.0 of the License,
 * or (at your option) any later version.
 *
 * base is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with base. If not, see <https://www.gnu.org/licenses/>.
 *
 * See <https://github.com/FeatureIDE/FeatJAR-base> for further information.
 */
package de.featjar.base.computation;

import de.featjar.base.FeatJAR;
import de.featjar.base.tree.structure.ATree;
import de.featjar.base.tree.structure.ITree;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Describes a deterministic (potentially complex or long-running) computation.
 * Primary implementation of {@link IComputation}.
 *
 * @param <T> the type of the computation result
 * @author Elias Kuiter
 */
public abstract class AComputation<T> extends ATree<IComputation<?>> implements IComputation<T> {

    protected Cache cache = FeatJAR.cache();

    protected AComputation(IComputation<?>... computations) {
        final Integer size = Dependency.getDependencyCount(getClass());
        assert size == computations.length;
        setChildren(List.of(computations));
    }

    protected AComputation(List<IComputation<?>> computations1, IComputation<?>... computations2) {
        final Integer size = Dependency.getDependencyCount(getClass());
        assert size == computations1.size() + computations2.length;
        ArrayList<IComputation<?>> computations = new ArrayList<>(size);
        computations.addAll(computations1);
        computations.addAll(List.of(computations2));
        setChildren(computations);
    }

    protected AComputation(Object... computations) {
        final Integer size = Dependency.getDependencyCount(getClass());
        ArrayList<IComputation<?>> computationList = new ArrayList<>(size);
        for (Object computation : computations) {
            unpackComputations(computationList, computation);
        }
        assert size == computationList.size();
        setChildren(computationList);
    }

    private void unpackComputations(List<IComputation<?>> computationList, Object element) {
        if (element instanceof IComputation<?>) {
            computationList.add((IComputation<?>) element);
        } else if (element instanceof Object[]) {
            for (Object computation : (Object[]) element) {
                unpackComputations(computationList, computation);
            }
        } else if (element instanceof List<?>) {
            for (Object computation : (List<?>) element) {
                unpackComputations(computationList, computation);
            }
        } else {
            throw new RuntimeException("Argument is not a computation: " + element.getClass());
        }
    }

    protected AComputation(AComputation<T> other) {}

    @Override
    public boolean equalsNode(IComputation<?> other) {
        return (getClass() == other.getClass());
    }

    @Override
    public int hashCodeNode() {
        return Objects.hash(getClass());
    }

    @Override
    public Cache getCache() {
        return cache;
    }

    /**
     * Sets the cache this computation should be stored and looked up in.
     *
     * @param cache the cache
     */
    public void setCache(Cache cache) {
        this.cache = Objects.requireNonNull(cache);
    }

    public <U> AComputation<T> setDependency(Dependency<U> dependency, IComputation<? extends U> computation) {
        replaceChild(dependency.getIndex(), computation);
        return this;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    @Override
    public ITree<IComputation<?>> cloneNode() {
        try {
            @SuppressWarnings("unchecked")
            final Class<? extends AComputation<T>> computationClass = (Class<? extends AComputation<T>>) getClass();
            return computationClass.getDeclaredConstructor(computationClass).newInstance(this);
        } catch (InstantiationException | NoSuchMethodException e) {
            FeatJAR.log().error(e);
            throw new UnsupportedOperationException(e);
        } catch (Exception e) {
            FeatJAR.log().error(e);
            throw new RuntimeException(e);
        }
    }
}