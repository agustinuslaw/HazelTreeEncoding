package matrixtree.structure;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.google.common.base.Strings;

import matrixtree.matrices.HazelPathMatrix;
import matrixtree.model.HazelTreePath;
import matrixtree.model.RationalInterval;
import matrixtree.validation.Precondition;

/**
 * List based matrix tree node rearranges automatically after remove and insert
 * operations. Thus there are no gaps between indexes.
 * 
 * @author Agustinus Lawandy
 * @since 2020-08-09
 */
public class ListHazelMatrixTreeNode<E extends Serializable> implements MutableMatrixTreeNode<E> {

	private static final long serialVersionUID = -2783094080814941618L;

	// transient to prevent this being serialized, which will cause stackoverflow
	private transient ListHazelMatrixTreeNode<E> parent;

	private E element;
	private long index;
	private HazelPathMatrix pathMatrix;
	private RationalInterval interval;
	private List<ListHazelMatrixTreeNode<E>> children;
	private Class<E> type;

	private final transient Supplier<List<ListHazelMatrixTreeNode<E>>> supplier = ArrayList::new;

	@SuppressWarnings("unchecked")
	public ListHazelMatrixTreeNode(ListHazelMatrixTreeNode<E> parent, E element, long index) {
		super();

		// simple setting
		this.parent = parent;
		this.index = index;
		this.element = element;
		this.children = supplier.get();

		// compute other values
		this.pathMatrix = computePathMatrix();
		this.interval = pathMatrix.asNestedInterval();
		this.type = (Class<E>) element.getClass();
	}

	@SuppressWarnings("unchecked")
	public ListHazelMatrixTreeNode(ListHazelMatrixTreeNode<E> parent, E element, HazelPathMatrix matrix) {
		super();

		// simple setting
		this.parent = parent;
		this.element = element;
		this.children = supplier.get();
		this.pathMatrix = matrix;
		this.interval = pathMatrix.asNestedInterval();

		// compute other values
		this.index = pathMatrix.computeIndex();
		this.type = (Class<E>) element.getClass();
	}

	@Override
	public RationalInterval getInterval() {
		return interval;
	}

	@Override
	public Class<E> getType() {
		return type;
	}

	public Supplier<List<ListHazelMatrixTreeNode<E>>> getSupplier() {
		return supplier;
	}

	@Override
	public HazelPathMatrix computePathMatrix() {
		if (parent != null) {
			// recompute path matrix depending on parent
			HazelPathMatrix parentM = parent.computePathMatrix();
			return new HazelPathMatrix(parentM, index);
		} else if (pathMatrix == null || isRoot()) {
			// become root according to index
			return new HazelTreePath(index).computePathMatrix();
		} else {
			// if there is already a path matrix and no use case fits.
			return pathMatrix;
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		@SuppressWarnings("unchecked")
		ListHazelMatrixTreeNode<E> other = (ListHazelMatrixTreeNode<E>) obj;
		return Objects.equals(pathMatrix, other.pathMatrix);
	}

	@Override
	public ListHazelMatrixTreeNode<E> getChildAt(int childIndex) {
		// When denominator == 1 : Reached top level already, division by 0 my occur
		Precondition.checkDomain(childIndex > 0, "childIndex", childIndex, "[1,Inf)");

		// this downcast is appropriate since only root elements can be long
		return children.get(positionOf(childIndex));
	}

	@Override
	public int getChildCount() {
		return children.size();
	}

	public List<MatrixTreeNode<E>> getChildren() {
		// safety copy to avoid aliasing error
		return children.stream().collect(Collectors.toList());
	}

	@Override
	public E getElement() {
		return element;
	}

	@Override
	public long getIndex() {
		return index;
	}

	@Override
	public ListHazelMatrixTreeNode<E> getParent() {
		return parent;
	}

	@Override
	public HazelPathMatrix getPathMatrix() {
		return pathMatrix;
	}

	@Override
	public int hashCode() {
		return Objects.hash(pathMatrix);
	}

	/**
	 * Converts between list position into matrix index. This is because index
	 * starts from 1. While the list element starts from 0
	 * <p>
	 * return position + 1;
	 * 
	 * @param position of list
	 * @return matrix index
	 */
	private int indexOf(int position) {
		return position + 1;
	}

	@Override
	public MutableMatrixTreeNode<E> insert(MutableMatrixTreeNode<E> child) {
		children.add((ListHazelMatrixTreeNode<E>) child);
		child.setParent(this);
		return child;
	}

	@Override
	public MutableMatrixTreeNode<E> add(E childElement) {

		return insert(childElement, indexOf(getChildCount()));
	}

	@Override
	public MutableMatrixTreeNode<E> insert(E childElement, int childIndex) {
		ListHazelMatrixTreeNode<E> inserted = new ListHazelMatrixTreeNode<>(this, childElement, childIndex);
		return insert(inserted);
	}

	/**
	 * Converts between matrix index into list position. This is because index
	 * starts from 1. While the list element starts from 0.
	 * <p>
	 * return index - 1;
	 * 
	 * @param index of matrix
	 * @return list position
	 */
	private int positionOf(int index) {
		return index - 1;
	}

	@Override
	public ListHazelMatrixTreeNode<E> remove(int childIndex) {
		if (children.size() >= positionOf(childIndex))
			return null;

		ListHazelMatrixTreeNode<E> removed = children.remove(positionOf(childIndex));
		removed.setParent(null);
		return removed;
	}

	@Override
	public boolean remove(MutableMatrixTreeNode<E> node) {
		boolean result = children.remove(node);
		node.setParent(null);
		return result;
	}

	@Override
	public void removeFromParent() {
		if (parent != null) {
			parent.remove(this);
		}
	}

	@Override
	public void setNodeElement(E element) {
		this.element = Objects.requireNonNull(element);
	}

	@Override
	public void setParent(MutableMatrixTreeNode<E> newParent) {

		// this should only be for direct parents, there musn't be any relocation
		this.parent = (ListHazelMatrixTreeNode<E>) newParent;
	}

	@Override
	public String toString() {
		return treeRepresentation(1);
	}

	private String treeRepresentation(int depth) {
		// base case:
		StringBuilder rootBuilder = new StringBuilder(lineRepresentation());
		// recursive case:
		String indent = Strings.repeat("  ", depth);
		for (ListHazelMatrixTreeNode<E> child : children) {
			rootBuilder.append(indent);
			rootBuilder.append(child.treeRepresentation(depth + 1));
		}

		return rootBuilder.toString();
	}

}
