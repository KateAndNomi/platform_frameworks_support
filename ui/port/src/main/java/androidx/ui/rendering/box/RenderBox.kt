package androidx.ui.rendering.box

import androidx.annotation.CallSuper
import androidx.ui.assert
import androidx.ui.engine.geometry.Offset
import androidx.ui.engine.geometry.Rect
import androidx.ui.engine.geometry.Size
import androidx.ui.foundation.assertions.FlutterError
import androidx.ui.foundation.diagnostics.DiagnosticPropertiesBuilder
import androidx.ui.foundation.diagnostics.DiagnosticsProperty
import androidx.ui.painting.Color
import androidx.ui.painting.Paint
import androidx.ui.painting.PaintingStyle
import androidx.ui.rendering.debugCheckIntrinsicSizes
import androidx.ui.rendering.debugPaintBaselinesEnabled
import androidx.ui.rendering.debugPaintPointersEnabled
import androidx.ui.rendering.debugPaintSizeEnabled
import androidx.ui.rendering.obj.PaintingContext
import androidx.ui.rendering.obj.RenderObject
import androidx.ui.rendering.obj.RenderObjectWithChildMixin
import androidx.ui.runtimeType

/**
 * A render object in a 2D Cartesian coordinate system.
 *
 * The [size] of each box is expressed as a width and a height. Each box has
 * its own coordinate system in which its upper left corner is placed at (0,
 * 0). The lower right corner of the box is therefore at (width, height). The
 * box contains all the points including the upper left corner and extending
 * to, but not including, the lower right corner.
 *
 * Box layout is performed by passing a [BoxConstraints] object down the tree.
 * The box constraints establish a min and max value for the child's width and
 * height. In determining its size, the child must respect the constraints
 * given to it by its parent.
 *
 * This protocol is sufficient for expressing a number of common box layout
 * data flows. For example, to implement a width-in-height-out data flow, call
 * your child's [layout] function with a set of box constraints with a tight
 * width value (and pass true for parentUsesSize). After the child determines
 * its height, use the child's height to determine your size.
 *
 * ## Writing a RenderBox subclass
 *
 * One would implement a new [RenderBox] subclass to describe a new layout
 * model, new paint model, new hit-testing model, or new semantics model, while
 * remaining in the Cartesian space defined by the [RenderBox] protocol.
 *
 * To create a new protocol, consider subclassing [RenderObject] instead.
 *
 * ### Constructors and properties of a new RenderBox subclass
 *
 * The constructor will typically take a named argument for each property of
 * the class. The value is then passed to a private field of the class and the
 * constructor asserts its correctness (e.g. if it should not be null, it
 * asserts it's not null).
 *
 * Properties have the form of a getter/setter/field group like the following:
 *
 * ```dart
 * AxisDirection get axis => _axis;
 * AxisDirection _axis;
 * set axis(AxisDirection value) {
 *   assert(value != null); // same check as in the constructor
 *   if (value == _axis)
 *     return;
 *   _axis = value;
 *   markNeedsLayout();
 * }
 * ```
 *
 * The setter will typically finish with either a call to [markNeedsLayout], if
 * the layout uses this property, or [markNeedsPaint], if only the painter
 * function does. (No need to call both, [markNeedsLayout] implies
 * [markNeedsPaint].)
 *
 * Consider layout and paint to be expensive; be conservative about calling
 * [markNeedsLayout] or [markNeedsPaint]. They should only be called if the
 * layout (or paint, respectively) has actually changed.
 *
 * ### Children
 *
 * If a render object is a leaf, that is, it cannot have any children, then
 * ignore this section. (Examples of leaf render objects are [RenderImage] and
 * [RenderParagraph].)
 *
 * For render objects with children, there are four possible scenarios:
 *
 * * A single [RenderBox] child. In this scenario, consider inheriting from
 *   [RenderProxyBox] (if the render object sizes itself to match the child) or
 *   [RenderShiftedBox] (if the child will be smaller than the box and the box
 *   will align the child inside itself).
 *
 * * A single child, but it isn't a [RenderBox]. Use the
 *   [RenderObjectWithChildMixin] mixin.
 *
 * * A single list of children. Use the [ContainerRenderObjectMixin] mixin.
 *
 * * A more complicated child model.
 *
 * #### Using RenderProxyBox
 *
 * By default, a [RenderProxyBox] render object sizes itself to fit its child, or
 * to be as small as possible if there is no child; it passes all hit testing
 * and painting on to the child, and intrinsic dimensions and baseline
 * measurements similarly are proxied to the child.
 *
 * A subclass of [RenderProxyBox] just needs to override the parts of the
 * [RenderBox] protocol that matter. For example, [RenderOpacity] just
 * overrides the paint method (and [alwaysNeedsCompositing] to reflect what the
 * paint method does, and the [visitChildrenForSemantics] method so that the
 * child is hidden from accessibility tools when it's invisible), and adds an
 * [RenderOpacity.opacity] field.
 *
 * [RenderProxyBox] assumes that the child is the size of the parent and
 * positioned at 0,0. If this is not true, then use [RenderShiftedBox] instead.
 *
 * See
 * [proxy_box.dart](https://github.com/flutter/flutter/blob/master/packages/flutter/lib/src/rendering/proxy_box.dart)
 * for examples of inheriting from [RenderProxyBox].
 *
 * #### Using RenderShiftedBox
 *
 * By default, a [RenderShiftedBox] acts much like a [RenderProxyBox] but
 * without assuming that the child is positioned at 0,0 (the actual position
 * recorded in the child's [parentData] field is used), and without providing a
 * default layout algorithm.
 *
 * See
 * [shifted_box.dart](https://github.com/flutter/flutter/blob/master/packages/flutter/lib/src/rendering/shifted_box.dart)
 * for examples of inheriting from [RenderShiftedBox].
 *
 * #### Kinds of children and child-specific data
 *
 * A [RenderBox] doesn't have to have [RenderBox] children. One can use another
 * subclass of [RenderObject] for a [RenderBox]'s children. See the discussion
 * at [RenderObject].
 *
 * Children can have additional data owned by the parent but stored on the
 * child using the [parentData] field. The class used for that data must
 * inherit from [ParentData]. The [setupParentData] method is used to
 * initialize the [parentData] field of a child when the child is attached.
 *
 * By convention, [RenderBox] objects that have [RenderBox] children use the
 * [BoxParentData] class, which has a [BoxParentData.offset] field to store the
 * position of the child relative to the parent. ([RenderProxyBox] does not
 * need this offset and therefore is an exception to this rule.)
 *
 * #### Using RenderObjectWithChildMixin
 *
 * If a render object has a single child but it isn't a [RenderBox], then the
 * [RenderObjectWithChildMixin] class, which is a mixin that will handle the
 * boilerplate of managing a child, will be useful.
 *
 * It's a generic class with one type argument, the type of the child. For
 * example, if you are building a `RenderFoo` class which takes a single
 * `RenderBar` child, you would use the mixin as follows:
 *
 * ```dart
 * class RenderFoo extends RenderBox
 *   with RenderObjectWithChildMixin<RenderBar> {
 *   // ...
 * }
 * ```
 *
 * Since the `RenderFoo` class itself is still a [RenderBox] in this case, you
 * still have to implement the [RenderBox] layout algorithm, as well as
 * features like intrinsics and baselines, painting, and hit testing.
 *
 * #### Using ContainerRenderObjectMixin
 *
 * If a render box can have multiple children, then the
 * [ContainerRenderObjectMixin] mixin can be used to handle the boilerplate. It
 * uses a linked list to model the children in a manner that is easy to mutate
 * dynamically and that can be walked efficiently. Random access is not
 * efficient in this model; if you need random access to the children consider
 * the next section on more complicated child models.
 *
 * The [ContainerRenderObjectMixin] class has two type arguments. The first is
 * the type of the child objects. The second is the type for their
 * [parentData]. The class used for [parentData] must itself have the
 * [ContainerParentDataMixin] class mixed into it; this is where
 * [ContainerRenderObjectMixin] stores the linked list. A [ParentData] class
 * can extend [ContainerBoxParentData]; this is essentially
 * [BoxParentData] mixed with [ContainerParentDataMixin]. For example, if a
 * `RenderFoo` class wanted to have a linked list of [RenderBox] children, one
 * might create a `FooParentData` class as follows:
 *
 * ```dart
 * class FooParentData extends ContainerBoxParentData<RenderBox> {
 *   // (any fields you might need for these children)
 * }
 * ```
 *
 * When using [ContainerRenderObjectMixin] in a [RenderBox], consider mixing in
 * [RenderBoxContainerDefaultsMixin], which provides a collection of utility
 * methods that implement common parts of the [RenderBox] protocol (such as
 * painting the children).
 *
 * The declaration of the `RenderFoo` class itself would thus look like this:
 *
 * ```dart
 * class RenderFlex extends RenderBox with
 *   ContainerRenderObjectMixin<RenderBox, FooParentData>,
 *   RenderBoxContainerDefaultsMixin<RenderBox, FooParentData> {
 *   // ...
 * }
 * ```
 *
 * When walking the children (e.g. during layout), the following pattern is
 * commonly used (in this case assuming that the children are all [RenderBox]
 * objects and that this render object uses `FooParentData` objects for its
 * children's [parentData] fields):
 *
 * ```dart
 * RenderBox child = firstChild;
 * while (child != null) {
 *   final FooParentData childParentData = child.parentData;
 *   // ...operate on child and childParentData...
 *   assert(child.parentData == childParentData);
 *   child = childParentData.nextSibling;
 * }
 * ```
 *
 * #### More complicated child models
 *
 * Render objects can have more complicated models, for example a map of
 * children keyed on an enum, or a 2D grid of efficiently randomly-accessible
 * children, or multiple lists of children, etc. If a render object has a model
 * that can't be handled by the mixins above, it must implement the
 * [RenderObject] child protocol, as follows:
 *
 * * Any time a child is removed, call [dropChild] with the child.
 *
 * * Any time a child is added, call [adoptChild] with the child.
 *
 * * Implement the [attach] method such that it calls [attach] on each child.
 *
 * * Implement the [detach] method such that it calls [detach] on each child.
 *
 * * Implement the [redepthChildren] method such that it calls [redepthChild]
 *   on each child.
 *
 * * Implement the [visitChildren] method such that it calls its argument for
 *   each child, typically in paint order (back-most to front-most).
 *
 * * Implement [debugDescribeChildren] such that it outputs a [DiagnosticsNode]
 *   for each child.
 *
 * Implementing these seven bullet points is essentially all that the two
 * aforementioned mixins do.
 *
 * ### Layout
 *
 * [RenderBox] classes implement a layout algorithm. They have a set of
 * constraints provided to them, and they size themselves based on those
 * constraints and whatever other inputs they may have (for example, their
 * children or properties).
 *
 * When implementing a [RenderBox] subclass, one must make a choice. Does it
 * size itself exclusively based on the constraints, or does it use any other
 * information in sizing itself? An example of sizing purely based on the
 * constraints would be growing to fit the parent.
 *
 * Sizing purely based on the constraints allows the system to make some
 * significant optimizations. Classes that use this approach should override
 * [sizedByParent] to return true, and then override [performResize] to set the
 * [size] using nothing but the constraints, e.g.:
 *
 * ```dart
 * @override
 * bool get sizedByParent => true;
 *
 * @override
 * void performResize() {
 *   size = constraints.smallest;
 * }
 * ```
 *
 * Otherwise, the size is set in the [performLayout] function.
 *
 * The [performLayout] function is where render boxes decide, if they are not
 * [sizedByParent], what [size] they should be, and also where they decide
 * where their children should be.
 *
 * #### Layout of RenderBox children
 *
 * The [performLayout] function should call the [layout] function of each (box)
 * child, passing it a [BoxConstraints] object describing the constraints
 * within which the child can render. Passing tight constraints (see
 * [BoxConstraints.isTight]) to the child will allow the rendering library to
 * apply some optimizations, as it knows that if the constraints are tight, the
 * child's dimensions cannot change even if the layout of the child itself
 * changes.
 *
 * If the [performLayout] function will use the child's size to affect other
 * aspects of the layout, for example if the render box sizes itself around the
 * child, or positions several children based on the size of those children,
 * then it must specify the `parentUsesSize` argument to the child's [layout]
 * function, setting it to true.
 *
 * This flag turns off some optimizations; algorithms that do not rely on the
 * children's sizes will be more efficient. (In particular, relying on the
 * child's [size] means that if the child is marked dirty for layout, the
 * parent will probably also be marked dirty for layout, unless the
 * [constraints] given by the parent to the child were tight constraints.)
 *
 * For [RenderBox] classes that do not inherit from [RenderProxyBox], once they
 * have laid out their children, should also position them, by setting the
 * [BoxParentData.offset] field of each child's [parentData] object.
 *
 * #### Layout of non-RenderBox children
 *
 * The children of a [RenderBox] do not have to be [RenderBox]es themselves. If
 * they use another protocol (as discussed at [RenderObject]), then instead of
 * [BoxConstraints], the parent would pass in the appropriate [Constraints]
 * subclass, and instead of reading the child's size, the parent would read
 * whatever the output of [layout] is for that layout protocol. The
 * `parentUsesSize` flag is still used to indicate whether the parent is going
 * to read that output, and optimizations still kick in if the child has tight
 * constraints (as defined by [Constraints.isTight]).
 *
 * ### Painting
 *
 * To describe how a render box paints, implement the [paint] method. It is
 * given a [PaintingContext] object and an [Offset]. The painting context
 * provides methods to affect the layer tree as well as a
 * [PaintingContext.canvas] which can be used to add drawing commands. The
 * canvas object should not be cached across calls to the [PaintingContext]'s
 * methods; every time a method on [PaintingContext] is called, there is a
 * chance that the canvas will change identity. The offset specifies the
 * position of the top left corner of the box in the coordinate system of the
 * [PaintingContext.canvas].
 *
 * To draw text on a canvas, use a [TextPainter].
 *
 * To draw an image to a canvas, use the [paintImage] method.
 *
 * A [RenderBox] that uses methods on [PaintingContext] that introduce new
 * layers should override the [alwaysNeedsCompositing] getter and set it to
 * true. If the object sometimes does and sometimes does not, it can have that
 * getter return true in some cases and false in others. In that case, whenever
 * the return value would change, call [markNeedsCompositingBitsUpdate]. (This
 * is done automatically when a child is added or removed, so you don't have to
 * call it explicitly if the [alwaysNeedsCompositing] getter only changes value
 * based on the presence or absence of children.)
 *
 * Anytime anything changes on the object that would cause the [paint] method
 * to paint something different (but would not cause the layout to change),
 * the object should call [markNeedsPaint].
 *
 * #### Painting children
 *
 * The [paint] method's `context` argument has a [PaintingContext.paintChild]
 * method, which should be called for each child that is to be painted. It
 * should be given a reference to the child, and an [Offset] giving the
 * position of the child relative to the parent.
 *
 * If the [paint] method applies a transform to the painting context before
 * painting children (or generally applies an additional offset beyond the
 * offset it was itself given as an argument), then the [applyPaintTransform]
 * method should also be overridden. That method must adjust the matrix that it
 * is given in the same manner as it transformed the painting context and
 * offset before painting the given child. This is used by the [globalToLocal]
 * and [localToGlobal] methods.
 *
 * #### Hit Tests
 *
 * Hit testing for render boxes is implemented by the [hitTest] method. The
 * default implementation of this method defers to [hitTestSelf] and
 * [hitTestChildren]. When implementing hit testing, you can either override
 * these latter two methods, or ignore them and just override [hitTest].
 *
 * The [hitTest] method itself is given an [Offset], and must return true if the
 * object or one of its children has absorbed the hit (preventing objects below
 * this one from being hit), or false if the hit can continue to other objects
 * below this one.
 *
 * For each child [RenderBox], the [hitTest] method on the child should be
 * called with the same [HitTestResult] argument and with the point transformed
 * into the child's coordinate space (in the same manner that the
 * [applyPaintTransform] method would). The default implementation defers to
 * [hitTestChildren] to call the children. [RenderBoxContainerDefaultsMixin]
 * provides a [RenderBoxContainerDefaultsMixin.defaultHitTestChildren] method
 * that does this assuming that the children are axis-aligned, not transformed,
 * and positioned according to the [BoxParentData.offset] field of the
 * [parentData]; more elaborate boxes can override [hitTestChildren]
 * accordingly.
 *
 * If the object is hit, then it should also add itself to the [HitTestResult]
 * object that is given as an argument to the [hitTest] method, using
 * [HitTestResult.add]. The default implementation defers to [hitTestSelf] to
 * determine if the box is hit. If the object adds itself before the children
 * can add themselves, then it will be as if the object was above the children.
 * If it adds itself after the children, then it will be as if it was below the
 * children. Entries added to the [HitTestResult] object should use the
 * [BoxHitTestEntry] class. The entries are subsequently walked by the system
 * in the order they were added, and for each entry, the target's [handleEvent]
 * method is called, passing in the [HitTestEntry] object.
 *
 * Hit testing cannot rely on painting having happened.
 *
 * ### Semantics
 *
 * For a render box to be accessible, implement the
 * [describeApproximatePaintClip] and [visitChildrenForSemantics] methods, and
 * the [semanticsAnnotator] getter. The default implementations are sufficient
 * for objects that only affect layout, but nodes that represent interactive
 * components or information (diagrams, text, images, etc) should provide more
 * complete implementations. For more information, see the documentation for
 * these members.
 *
 * ### Intrinsics and Baselines
 *
 * The layout, painting, hit testing, and semantics protocols are common to all
 * render objects. [RenderBox] objects must implement two additional protocols:
 * intrinsic sizing and baseline measurements.
 *
 * There are four methods to implement for intrinsic sizing, to compute the
 * minimum and maximum intrinsic width and height of the box. The documentation
 * for these methods discusses the protocol in detail:
 * [computeMinIntrinsicWidth], [computeMaxIntrinsicWidth],
 * [computeMinIntrinsicHeight], [computeMaxIntrinsicHeight].
 *
 * In addition, if the box has any children, it must implement
 * [computeDistanceToActualBaseline]. [RenderProxyBox] provides a simple
 * implementation that forwards to the child; [RenderShiftedBox] provides an
 * implementation that offsets the child's baseline information by the position
 * of the child relative to the parent. If you do not inherited from either of
 * these classes, however, you must implement the algorithm yourself.
 */
// (Migration/Andrey): Replaced ": RenderObject"  with ": RenderObjectWithChildMixin" to make
//  RenderProxyBoxMixin works.
abstract class RenderBox : RenderObjectWithChildMixin<RenderBox>() {
    override fun setupParentData(child: RenderObject) {
        if (child.parentData !is BoxParentData)
            child.parentData = BoxParentData()
    }

    var _cachedIntrinsicDimensions: MutableMap<_IntrinsicDimensionsCacheEntry, Double>? = null

    fun _computeIntrinsicDimension(
        dimension: _IntrinsicDimension,
        argument: Double,
        computer: (Double) -> Double
    ): Double {
        // performResize should not depend on anything except the incoming constraints
        assert(RenderObject.debugCheckingIntrinsics || !debugDoingThisResize)
        var shouldCache = true
        assert {
            // we don't want the checked-mode intrinsic tests to affect
            // who gets marked dirty, etc.
            if (RenderObject.debugCheckingIntrinsics)
                shouldCache = false
            true
        }
        if (shouldCache) {
            if (_cachedIntrinsicDimensions == null) {
                _cachedIntrinsicDimensions = mutableMapOf()
            }
            return _cachedIntrinsicDimensions!!.getOrPut(
                _IntrinsicDimensionsCacheEntry(dimension, argument), { computer(argument) }
            )
        }
        return computer(argument)
    }

    /**
     * Returns the minimum width that this box could be without failing to
     * correctly paint its contents within itself, without clipping.
     *
     * The height argument may give a specific height to assume. The given height
     * can be infinite, meaning that the intrinsic width in an unconstrained
     * environment is being requested. The given height should never be negative
     * or null.
     *
     * This function should only be called on one's children. Calling this
     * function couples the child with the parent so that when the child's layout
     * changes, the parent is notified (via [markNeedsLayout]).
     *
     * Calling this function is expensive and as it can result in O(N^2)
     * behavior.
     *
     * Do not override this method. Instead, implement [computeMinIntrinsicWidth].
     */
    @CallSuper
    fun getMinIntrinsicWidth(height: Double?): Double {
        assert {
            if (height == null) {
                throw FlutterError(
                    "The height argument to getMinIntrinsicWidth was null.\n" +
                            "The argument to getMinIntrinsicWidth must not be " +
                            "negative or null. If you do not have a specific height in mind, " +
                            "then pass double.infinity instead."
                )
            }
            if (height < 0.0) {
                throw FlutterError(
                    "The height argument to getMinIntrinsicWidth was negative.\n" +
                            "The argument to getMinIntrinsicWidth must not be negative or null. " +
                            "If you perform computations on another height before passing it to " +
                            "getMinIntrinsicWidth, consider using math.max() or double.clamp() " +
                            "to force the value into the valid range."
                )
            }
            true
        }
        return _computeIntrinsicDimension(
            _IntrinsicDimension.minWidth, height!!,
            ::computeMinIntrinsicWidth
        )
    }

    /**
     * Computes the value returned by [getMinIntrinsicWidth]. Do not call this
     * function directly, instead, call [getMinIntrinsicWidth].
     *
     * Override in subclasses that implement [performLayout]. This method should
     * return the minimum width that this box could be without failing to
     * correctly paint its contents within itself, without clipping.
     *
     * If the layout algorithm is independent of the context (e.g. it always
     * tries to be a particular size), or if the layout algorithm is
     * width-in-height-out, or if the layout algorithm uses both the incoming
     * width and height constraints (e.g. it always sizes itself to
     * [BoxConstraints.biggest]), then the `height` argument should be ignored.
     *
     * If the layout algorithm is strictly height-in-width-out, or is
     * height-in-width-out when the width is unconstrained, then the height
     * argument is the height to use.
     *
     * The `height` argument will never be negative or null. It may be infinite.
     *
     * If this algorithm depends on the intrinsic dimensions of a child, the
     * intrinsic dimensions of that child should be obtained using the functions
     * whose names start with `get`, not `compute`.
     *
     * This function should never return a negative or infinite value.
     *
     * ## Examples
     *
     * ### Text
     *
     * Text is the canonical example of a width-in-height-out algorithm. The
     * `height` argument is therefore ignored.
     *
     * Consider the string "Hello World" The _maximum_ intrinsic width (as
     * returned from [computeMaxIntrinsicWidth]) would be the width of the string
     * with no line breaks.
     *
     * The minimum intrinsic width would be the width of the widest word, "Hello"
     * or "World". If the text is rendered in an even narrower width, however, it
     * might still not overflow. For example, maybe the rendering would put a
     * line-break half-way through the words, as in "Hel⁞lo⁞Wor⁞ld". However,
     * this wouldn't be a _correct_ rendering, and [computeMinIntrinsicWidth] is
     * supposed to render the minimum width that the box could be without failing
     * to _correctly_ paint the contents within itself.
     *
     * The minimum intrinsic _height_ for a given width smaller than the minimum
     * intrinsic width could therefore be greater than the minimum intrinsic
     * height for the minimum intrinsic width.
     *
     * ### Viewports (e.g. scrolling lists)
     *
     * Some render boxes are intended to clip their children. For example, the
     * render box for a scrolling list might always size itself to its parents'
     * size (or rather, to the maximum incoming constraints), regardless of the
     * children's sizes, and then clip the children and position them based on
     * the current scroll offset.
     *
     * The intrinsic dimensions in these cases still depend on the children, even
     * though the layout algorithm sizes the box in a way independent of the
     * children. It is the size that is needed to paint the box's contents (in
     * this case, the children) _without clipping_ that matters.
     *
     * ### When the intrinsic dimensions cannot be known
     *
     * There are cases where render objects do not have an efficient way to
     * compute their intrinsic dimensions. For example, it may be prohibitively
     * expensive to reify and measure every child of a lazy viewport (viewports
     * generally only instantiate the actually visible children), or the
     * dimensions may be computed by a callback about which the render object
     * cannot reason.
     *
     * In such cases, it may be impossible (or at least impractical) to actually
     * return a valid answer. In such cases, the intrinsic functions should throw
     * when [RenderObject.debugCheckingIntrinsics] is false and asserts are
     * enabled, and return 0.0 otherwise.
     *
     * See the implementations of [LayoutBuilder] or [RenderViewportBase] for
     * examples (in particular,
     * [RenderViewportBase.debugThrowIfNotCheckingIntrinsics]).
     *
     * ### Aspect-ratio-driven boxes
     *
     * Some boxes always return a fixed size based on the constraints. For these
     * boxes, the intrinsic functions should return the appropriate size when the
     * incoming `height` or `width` argument is finite, treating that as a tight
     * constraint in the respective direction and treating the other direction's
     * constraints as unbounded. This is because the definitions of
     * [computeMinIntrinsicWidth] and [computeMinIntrinsicHeight] are in terms of
     * what the dimensions _could be_, and such boxes can only be one size in
     * such cases.
     *
     * When the incoming argument is not finite, then they should return the
     * actual intrinsic dimensions based on the contents, as any other box would.
     */
    protected open fun computeMinIntrinsicWidth(height: Double): Double {
        return 0.0
    }

    /**
     * Returns the smallest width beyond which increasing the width never
     * decreases the preferred height. The preferred height is the value that
     * would be returned by [getMinIntrinsicHeight] for that width.
     *
     * The height argument may give a specific height to assume. The given height
     * can be infinite, meaning that the intrinsic width in an unconstrained
     * environment is being requested. The given height should never be negative
     * or null.
     *
     * This function should only be called on one's children. Calling this
     * function couples the child with the parent so that when the child's layout
     * changes, the parent is notified (via [markNeedsLayout]).
     *
     * Calling this function is expensive and as it can result in O(N^2)
     * behavior.
     *
     * Do not override this method. Instead, implement
     * [computeMaxIntrinsicWidth].
     */
    @CallSuper
    fun getMaxIntrinsicWidth(height: Double?): Double {
        assert {
            if (height == null) {
                throw FlutterError(
                    "The height argument to getMaxIntrinsicWidth was null.\n" +
                            "The argument to getMaxIntrinsicWidth must not be negative or null. " +
                            "If you do not have a specific height in mind, " +
                            "then pass double.infinity instead."
                )
            }
            if (height < 0.0) {
                throw FlutterError(
                    "The height argument to getMaxIntrinsicWidth was negative.\n" +
                            "The argument to getMaxIntrinsicWidth must not be negative or null. " +
                            "If you perform computations on another height before passing it to " +
                            "getMaxIntrinsicWidth, consider using math.max() or double.clamp() " +
                            "to force the value into the valid range."
                )
            }
            true
        }
        return _computeIntrinsicDimension(
            _IntrinsicDimension.maxWidth, height!!,
            ::computeMaxIntrinsicWidth
        )
    }

    /**
     * Computes the value returned by [getMaxIntrinsicWidth]. Do not call this
     * function directly, instead, call [getMaxIntrinsicWidth].
     *
     * Override in subclasses that implement [performLayout]. This should return
     * the smallest width beyond which increasing the width never decreases the
     * preferred height. The preferred height is the value that would be returned
     * by [computeMinIntrinsicHeight] for that width.
     *
     * If the layout algorithm is strictly height-in-width-out, or is
     * height-in-width-out when the width is unconstrained, then this should
     * return the same value as [computeMinIntrinsicWidth] for the same height.
     *
     * Otherwise, the height argument should be ignored, and the returned value
     * should be equal to or bigger than the value returned by
     * [computeMinIntrinsicWidth].
     *
     * The `height` argument will never be negative or null. It may be infinite.
     *
     * The value returned by this method might not match the size that the object
     * would actually take. For example, a [RenderBox] subclass that always
     * exactly sizes itself using [BoxConstraints.biggest] might well size itself
     * bigger than its max intrinsic size.
     *
     * If this algorithm depends on the intrinsic dimensions of a child, the
     * intrinsic dimensions of that child should be obtained using the functions
     * whose names start with `get`, not `compute`.
     *
     * This function should never return a negative or infinite value.
     *
     * See also examples in the definition of [computeMinIntrinsicWidth].
     */
    protected open fun computeMaxIntrinsicWidth(height: Double): Double {
        return 0.0
    }

    /** Returns the minimum height that this box could be without failing to
     * correctly paint its contents within itself, without clipping.
     *
     * The width argument may give a specific width to assume. The given width
     * can be infinite, meaning that the intrinsic height in an unconstrained
     * environment is being requested. The given width should never be negative
     * or null.
     *
     * This function should only be called on one's children. Calling this
     * function couples the child with the parent so that when the child's layout
     * changes, the parent is notified (via [markNeedsLayout]).
     *
     * Calling this function is expensive and as it can result in O(N^2)
     * behavior.
     *
     * Do not override this method. Instead, implement
     * [computeMinIntrinsicHeight].
     */
    @CallSuper
    fun getMinIntrinsicHeight(width: Double?): Double {
        assert {
            if (width == null) {
                throw FlutterError(
                    "The width argument to getMinIntrinsicHeight was null.\n" +
                            "The argument to getMinIntrinsicHeight must not be negative or null. " +
                            "If you do not have a specific width in mind, " +
                            "then pass double.infinity instead."
                )
            }
            if (width < 0.0) {
                throw FlutterError(
                    "The width argument to getMinIntrinsicHeight was negative.\n" +
                            "The argument to getMinIntrinsicHeight must not be negative or null. " +
                            "If you perform computations on another width before passing it to " +
                            "getMinIntrinsicHeight, consider using math.max() or double.clamp() " +
                            "to force the value into the valid range."
                )
            }
            true
        }
        return _computeIntrinsicDimension(
            _IntrinsicDimension.minHeight, width!!,
            ::computeMinIntrinsicHeight
        )
    }

    /**
     * Computes the value returned by [getMinIntrinsicHeight]. Do not call this
     * function directly, instead, call [getMinIntrinsicHeight].
     *
     * Override in subclasses that implement [performLayout]. Should return the
     * minimum height that this box could be without failing to correctly paint
     * its contents within itself, without clipping.
     *
     * If the layout algorithm is independent of the context (e.g. it always
     * tries to be a particular size), or if the layout algorithm is
     * height-in-width-out, or if the layout algorithm uses both the incoming
     * height and width constraints (e.g. it always sizes itself to
     * [BoxConstraints.biggest]), then the `width` argument should be ignored.
     *
     * If the layout algorithm is strictly width-in-height-out, or is
     * width-in-height-out when the height is unconstrained, then the width
     * argument is the width to use.
     *
     * The `width` argument will never be negative or null. It may be infinite.
     *
     * If this algorithm depends on the intrinsic dimensions of a child, the
     * intrinsic dimensions of that child should be obtained using the functions
     * whose names start with `get`, not `compute`.
     *
     * This function should never return a negative or infinite value.
     *
     * See also examples in the definition of [computeMinIntrinsicWidth].
     */
    protected open fun computeMinIntrinsicHeight(width: Double): Double {
        return 0.0
    }

    /**
     * Returns the smallest height beyond which increasing the height never
     * decreases the preferred width. The preferred width is the value that
     * would be returned by [getMinIntrinsicWidth] for that height.
     *
     * The width argument may give a specific width to assume. The given width
     * can be infinite, meaning that the intrinsic height in an unconstrained
     * environment is being requested. The given width should never be negative
     * or null.
     *
     * This function should only be called on one's children. Calling this
     * function couples the child with the parent so that when the child's layout
     * changes, the parent is notified (via [markNeedsLayout]).
     *
     * Calling this function is expensive and as it can result in O(N^2)
     * behavior.
     *
     * Do not override this method. Instead, implement
     * [computeMaxIntrinsicHeight].
     */
    @CallSuper
    fun getMaxIntrinsicHeight(width: Double?): Double {
        assert {
            if (width == null) {
                throw FlutterError(
                    "The width argument to getMaxIntrinsicHeight was null.\n" +
                            "The argument to getMaxIntrinsicHeight must not be negative or null. " +
                            "If you do not have a specific width in mind, " +
                            "then pass double.infinity instead."
                )
            }
            if (width < 0.0) {
                throw FlutterError(
                    "The width argument to getMaxIntrinsicHeight was negative.\n" +
                            "The argument to getMaxIntrinsicHeight must not be negative or null. " +
                            "If you perform computations on another width before passing it to " +
                            "getMaxIntrinsicHeight, consider using math.max() or double.clamp() " +
                            "to force the value into the valid range."
                )
            }
            true
        }
        return _computeIntrinsicDimension(
            _IntrinsicDimension.maxHeight, width!!,
            ::computeMaxIntrinsicHeight
        )
    }

    /**
     * Computes the value returned by [getMaxIntrinsicHeight]. Do not call this
     * function directly, instead, call [getMaxIntrinsicHeight].
     *
     * Override in subclasses that implement [performLayout]. Should return the
     * smallest height beyond which increasing the height never decreases the
     * preferred width. The preferred width is the value that would be returned
     * by [computeMinIntrinsicWidth] for that height.
     *
     * If the layout algorithm is strictly width-in-height-out, or is
     * width-in-height-out when the height is unconstrained, then this should
     * return the same value as [computeMinIntrinsicHeight] for the same width.
     *
     * Otherwise, the width argument should be ignored, and the returned value
     * should be equal to or bigger than the value returned by
     * [computeMinIntrinsicHeight].
     *
     * The `width` argument will never be negative or null. It may be infinite.
     *
     * The value returned by this method might not match the size that the object
     * would actually take. For example, a [RenderBox] subclass that always
     * exactly sizes itself using [BoxConstraints.biggest] might well size itself
     * bigger than its max intrinsic size.
     *
     * If this algorithm depends on the intrinsic dimensions of a child, the
     * intrinsic dimensions of that child should be obtained using the functions
     * whose names start with `get`, not `compute`.
     *
     * This function should never return a negative or infinite value.
     *
     * See also examples in the definition of [computeMinIntrinsicWidth].
     */
    protected open fun computeMaxIntrinsicHeight(width: Double): Double {
        return 0.0
    }

    // Whether this render object has undergone layout and has a [size].
    val hasSize get() = _size != null

    /**
     * The size of this render box computed during layout.
     *
     * This value is stale whenever this object is marked as needing layout.
     * During [performLayout], do not read the size of a child unless you pass
     * true for parentUsesSize when calling the child's [layout] function.
     *
     * The size of a box should be set only during the box's [performLayout] or
     * [performResize] functions. If you wish to change the size of a box outside
     * of those functions, call [markNeedsLayout] instead to schedule a layout of
     * the box.
     */
    var size: Size?
        get() {
            assert(hasSize, { "RenderBox was not laid out: ${toString()}" })
            assert {
                if (_size is _DebugSize) {
                    val _size = this._size as _DebugSize
                    assert(_size._owner == this)
                    if (RenderObject.debugActiveLayout != null) {
                        // We are always allowed to access our own size (for print debugging
                        // and asserts if nothing else). Other than us, the only object that's
                        // allowed to read our size is our parent, if they've said they will.
                        // If you hit this assert trying to access a child's size, pass
                        // "parentUsesSize: true" to that child's layout().
                        assert(debugDoingThisResize || debugDoingThisLayout ||
                                (RenderObject.debugActiveLayout == parent &&
                                        _size._canBeUsedByParent))
                    }
                    assert(_size == this._size)
                }
                true
            }
            return _size
        }
        // Setting the size, in checked mode, triggers some analysis of the render box,
        // as implemented by [debugAssertDoesMeetConstraints], including calling the intrinsic
        // sizing methods and checking that they meet certain invariants.
        protected set(value) {
            // TODO(Migration/xbhatnag): Hack because 'value' is immutable
            var mutableValue = value
            assert(!(debugDoingThisResize && debugDoingThisLayout))
            assert(sizedByParent || !debugDoingThisResize)
            assert {
                if ((sizedByParent && debugDoingThisResize) ||
                    (!sizedByParent && debugDoingThisLayout)
                )
                    return@assert true
                assert(!debugDoingThisResize)
                val contract: String
                val violation: String
                val hint: String
                if (debugDoingThisLayout) {
                    assert(sizedByParent)
                    violation = "It appears that the size setter was called from performLayout()."
                    hint = ""
                } else {
                    violation = "The size setter was called from outside layout " +
                            "(neither performResize() nor performLayout() " +
                            "were being run for this object)."
                    if (owner != null && owner!!.debugDoingLayout)
                        hint = "Only the object itself can set its size. " +
                                "It is a contract violation for other objects to set it."
                    else
                        hint = ""
                }
                if (sizedByParent)
                    contract = "Because this RenderBox has sizedByParent set to true, " +
                            "it must set its size in performResize()."
                else
                    contract = "Because this RenderBox has sizedByParent set to false, " +
                            "it must set its size in performLayout()."
                throw FlutterError(
                    "RenderBox size setter called incorrectly.\n" +
                            "$violation\n" +
                            "$hint\n" +
                            "$contract\n" +
                            "The RenderBox in question is:\n" +
                            "  $this"
                )
            }
            assert {
                // TODO(Migration/xbhatnag): Double check if this should be non-null
                mutableValue = debugAdoptSize(mutableValue!!)
                true
            }
            _size = mutableValue
            assert { debugAssertDoesMeetConstraints(); true; }
        }

    var _size: Size? = null

    /** Claims ownership of the given [Size].
     *
     * In debug mode, the [RenderBox] class verifies that [Size] objects obtained
     * from other [RenderBox] objects are only used according to the semantics of
     * the [RenderBox] protocol, namely that a [Size] from a [RenderBox] can only
     * be used by its parent, and then only if `parentUsesSize` was set.
     *
     * Sometimes, a [Size] that can validly be used ends up no longer being valid
     * over time. The common example is a [Size] taken from a child that is later
     * removed from the parent. In such cases, this method can be called to first
     * check whether the size can legitimately be used, and if so, to then create
     * a new [Size] that can be used going forward, regardless of what happens to
     * the original owner.
     */
    fun debugAdoptSize(value: Size): Size {
        var result = value
        assert {
            if (value is _DebugSize) {
                if (value._owner != this) {
                    if (value._owner.parent != this) {
                        throw FlutterError(
                                "The size property was assigned a size inappropriately.\n" +
                        "The following render object:\n" +
                        "  $this\n" +
                        "...was assigned a size obtained from:\n" +
                        "  ${value._owner}\n" +
                        "However, this second render object is not, or is no longer, a " +
                        "child of the first, and it is therefore a violation of the " +
                        "RenderBox layout protocol to use that size in the layout of the " +
                        "first render object.\n" +
                        "If the size was obtained at a time where it was valid to read " +
                        "the size (because the second render object above was a child " +
                        "of the first at the time), then it should be adopted using " +
                        "debugAdoptSize at that time.\n" +
                        "If the size comes from a grandchild or a render object from an " +
                        "entirely different part of the render tree, then there is no " +
                        "way to be notified when the size changes and therefore attempts " +
                        "to read that size are almost certainly a source of bugs. A different " +
                        "approach should be used."
                        )
                    }
                    if (!value._canBeUsedByParent) {
                        throw FlutterError(
                                "A child\'s size was used without setting parentUsesSize.\n" +
                        "The following render object:\n" +
                        "  $this\n" +
                        "...was assigned a size obtained from its child:\n" +
                        "  ${value._owner}\n" +
                        "However, when the child was laid out, the parentUsesSize argument " +
                        "was not set or set to false. Subsequently this transpired to be " +
                        "inaccurate: the size was nonetheless used by the parent.\n" +
                        "It is important to tell the framework if the size will be used or not " +
                        "as several important performance optimizations can be made if the " +
                        "size will not be used by the parent."
                        )
                    }
                }
            }
            result = _DebugSize(value, this, debugCanParentUseSize)
            true
        }
        return result
    }

    override val semanticBounds: Rect get() = Offset.zero.and(size!!)

    override fun debugResetSize() {
        // updates the value of size._canBeUsedByParent if necessary
        size = size
    }

    // TODO(Migration/xbhatnag): Needs TextBaseline
//    Map<TextBaseline, double> _cachedBaselines;

    companion object {
        var _debugDoingBaseline = false
        fun _debugSetDoingBaseline(value: Boolean): Boolean {
            _debugDoingBaseline = value
            return true
        }
    }

    // TODO(Migration/andrey): Needs TextBaseline
//    /// Returns the distance from the y-coordinate of the position of the box to
//    /// the y-coordinate of the first given baseline in the box's contents.
//    ///
//    /// Used by certain layout models to align adjacent boxes on a common
//    /// baseline, regardless of padding, font size differences, etc. If there is
//    /// no baseline, this function returns the distance from the y-coordinate of
//    /// the position of the box to the y-coordinate of the bottom of the box
//    /// (i.e., the height of the box) unless the caller passes true
//    /// for `onlyReal`, in which case the function returns null.
//    ///
//    /// Only call this function after calling [layout] on this box. You
//    /// are only allowed to call this from the parent of this box during
//    /// that parent's [performLayout] or [paint] functions.
//    double getDistanceToBaseline(TextBaseline baseline, { bool onlyReal: false }) {
//        assert(!debugNeedsLayout);
//        assert(!_debugDoingBaseline);
//        assert(() {
//            final RenderObject parent = this.parent;
//            if (owner.debugDoingLayout)
//                return (RenderObject.debugActiveLayout == parent) && parent.debugDoingThisLayout;
//            if (owner.debugDoingPaint)
//                return ((RenderObject.debugActivePaint == parent) && parent.debugDoingThisPaint) ||
//                        ((RenderObject.debugActivePaint == this) && debugDoingThisPaint);
//            assert(parent == this.parent);
//            return false;
//        }());
//        assert(_debugSetDoingBaseline(true));
//        final double result = getDistanceToActualBaseline(baseline);
//        assert(_debugSetDoingBaseline(false));
//        if (result == null && !onlyReal)
//            return size.height;
//        return result;
//    }
//
//    /// Calls [computeDistanceToActualBaseline] and caches the result.
//    ///
//    /// This function must only be called from [getDistanceToBaseline] and
//    /// [computeDistanceToActualBaseline]. Do not call this function directly from
//    /// outside those two methods.
//    @protected
//    @mustCallSuper
//    double getDistanceToActualBaseline(TextBaseline baseline) {
//        assert(_debugDoingBaseline);
//        _cachedBaselines ??= <TextBaseline, double>{};
//        _cachedBaselines.putIfAbsent(baseline, () => computeDistanceToActualBaseline(baseline));
//        return _cachedBaselines[baseline];
//    }
//
//    /// Returns the distance from the y-coordinate of the position of the box to
//    /// the y-coordinate of the first given baseline in the box's contents, if
//    /// any, or null otherwise.
//    ///
//    /// Do not call this function directly. Instead, call [getDistanceToBaseline]
//    /// if you need to know the baseline of a child from an invocation of
//    /// [performLayout] or [paint] and call [getDistanceToActualBaseline] if you
//    /// are implementing [computeDistanceToActualBaseline] and need to defer to a
//    /// child.
//    ///
//    /// Subclasses should override this method to supply the distances to their
//    /// baselines.
//    @protected
//    double computeDistanceToActualBaseline(TextBaseline baseline) {
//        assert(_debugDoingBaseline);
//        return null;
//    }
//
    // The box constraints most recently received from the parent.
    override val constraints: BoxConstraints?
        get() = super.constraints as BoxConstraints?

    override fun debugAssertDoesMeetConstraints() {
        assert(constraints != null)
        assert {
            val runtimeType = this.runtimeType()
            if (!hasSize) {
                // this is called in the size= setter during layout, but in that case we have a size
                assert(!debugNeedsLayout)
                var contract: String
                if (sizedByParent)
                    contract = "Because this RenderBox has sizedByParent set to true, " +
                            "it must set its size in performResize().\n"
                else
                    contract = "Because this RenderBox has sizedByParent set to false, " +
                            "it must set its size in performLayout().\n"
                throw FlutterError(
                    "RenderBox did not set its size during layout.\n" +
                            "$contract" +
                            "It appears that this did not happen; layout completed, " +
                            "but the size property is still null.\n" +
                            "The RenderBox in question is:\n" +
                            "  $this"
                )
            }
            // verify that the size is not infinite
            if (!_size!!.isFinite()) {
                val information = StringBuffer()
                if (!constraints!!.hasBoundedWidth) {
                    var node: RenderBox = this
                    while (!node.constraints!!.hasBoundedWidth && node.parent is RenderBox)
                    // TODO(Migration/xbhatnag): Explicit cast to RenderBox
                        node = node.parent as RenderBox
                    information.appendln(
                        "The nearest ancestor providing an unbounded width constraint is:"
                    )
                    information.appendln("  ")
                    // TODO(Migraiton/xbhatnag): toStringShallow not implemented
                    // information.appendln(node.toStringShallow(joiner: '\n  '));
                }
                if (!constraints!!.hasBoundedHeight) {
                    var node: RenderBox = this
                    while (!node.constraints!!.hasBoundedHeight && node.parent is RenderBox)
                    // TODO(Migration/xbhatnag): Explicit cast to RenderBox
                        node = node.parent as RenderBox
                    information.appendln(
                        "The nearest ancestor providing an unbounded height constraint is:"
                    )
                    information.appendln("  ")
                    // TODO(Migraiton/xbhatnag): toStringShallow not implemented
                    // information.appendln(node.toStringShallow(joiner: '\n  '));
                }
                throw FlutterError(
                    "$runtimeType object was given an infinite size during layout.\n" +
                            "This probably means that it is a render object that tries to be " +
                            "as big as possible, but it was put inside another render object " +
                            "that allows its children to pick their own size.\n" +
                            "$information" +
                            "The constraints that applied to the $runtimeType were:\n" +
                            "  $constraints\n" +
                            "The exact size it was given was:\n" +
                            "  $_size\n" +
                            "See https://flutter.io/layout/ for more information."
                )
            }
            // verify that the size is within the constraints
            if (!constraints!!.isSatisfiedBy(_size!!)) {
                throw FlutterError(
                    "$runtimeType does not meet its constraints.\n" +
                            "Constraints: $constraints\n" +
                            "Size: $_size\n" +
                            "If you are not writing your own RenderBox subclass, then this " +
                            "is not your fault. " +
                            "Contact support: https://github.com/flutter/flutter/issues/new"
                )
            }
            if (debugCheckIntrinsicSizes) {
                // verify that the intrinsics are sane
                assert(!RenderObject.debugCheckingIntrinsics)
                RenderObject.debugCheckingIntrinsics = true
                val failures = StringBuffer()
                var failureCount = 0

                fun testIntrinsic(
                    function: (Double) -> Double,
                    name: String,
                    constraint: Double
                ): Double {
                    val result = function(constraint)
                    if (result < 0) {
                        failures.appendln(
                            " * $name($constraint) returned a negative value: $result"
                        )
                        failureCount += 1
                    }
                    if (!result.isFinite()) {
                        failures.appendln(
                            " * $name($constraint) returned a non-finite value: $result"
                        )
                        failureCount += 1
                    }
                    return result
                }

                fun testIntrinsicsForValues(
                    getMin: (Double) -> Double,
                    getMax: (Double) -> Double,
                    name: String,
                    constraint: Double
                ) {
                    val min = testIntrinsic(getMin, "getMinIntrinsic$name", constraint)
                    val max = testIntrinsic(getMax, "getMaxIntrinsic$name", constraint)
                    if (min > max) {
                        failures.appendln(" * getMinIntrinsic$name($constraint) returned a " +
                                "larger value ($min) than " +
                                "getMaxIntrinsic$name($constraint) ($max)"
                        )
                        failureCount += 1
                    }
                }

                testIntrinsicsForValues(
                    ::getMinIntrinsicWidth, ::getMaxIntrinsicWidth, "Width",
                    Double.POSITIVE_INFINITY
                )
                testIntrinsicsForValues(
                    ::getMinIntrinsicHeight, ::getMaxIntrinsicHeight, "Height",
                    Double.POSITIVE_INFINITY
                )
                if (constraints!!.hasBoundedWidth)
                    testIntrinsicsForValues(
                        ::getMinIntrinsicWidth, ::getMaxIntrinsicWidth, "Width",
                        constraints!!.maxWidth
                    )
                if (constraints!!.hasBoundedHeight)
                    testIntrinsicsForValues(
                        ::getMinIntrinsicHeight, ::getMaxIntrinsicHeight,
                        "Height", constraints!!.maxHeight
                    )

                // TODO(ianh): Test that values are internally consistent in more ways than the above.

                RenderObject.debugCheckingIntrinsics = false
                if (failures.isNotEmpty()) {
                    assert(failureCount > 0)
                    throw FlutterError(
                            "The intrinsic dimension methods of the $runtimeType class " +
                            "returned values that violate the intrinsic protocol contract.\n" +
                            "The following " +
                            "${if (failureCount > 1) "failures" else "failure"} was detected:\n" +
                            "$failures" +
                            "If you are not writing your own RenderBox subclass, " +
                            "then this is not\n" + "your fault. " +
                            "Contact support: https://github.com/flutter/flutter/issues/new"
                    )
                }
            }
            true
        }
    }

    // TODO(Migration/xbhatnag): Needs TextBaseline
//    @override
//    void markNeedsLayout() {
//        if ((_cachedBaselines != null && _cachedBaselines.isNotEmpty) ||
//                (_cachedIntrinsicDimensions != null && _cachedIntrinsicDimensions.isNotEmpty)) {
//            // If we have cached data, then someone must have used our data.
//            // Since the parent will shortly be marked dirty, we can forget that they
//            // used the baseline and/or intrinsic dimensions. If they use them again,
//            // then we'll fill the cache again, and if we get dirty again, we'll
//            // notify them again.
//            _cachedBaselines?.clear();
//            _cachedIntrinsicDimensions?.clear();
//            if (parent is RenderObject) {
//                markParentNeedsLayout();
//                return;
//            }
//        }
//        super.markNeedsLayout();
//    }

    override fun performResize() {
        // default behavior for subclasses that have sizedByParent = true
        size = constraints!!.smallest
        assert(size!!.isFinite())
    }

    override fun performLayout() {
        assert {
            if (!sizedByParent) {
                throw FlutterError(
                    "${this.runtimeType()} did not implement performLayout().\n" +
                            "RenderBox subclasses need to either override performLayout() to " +
                            "set a size and lay out any children, or, set sizedByParent to " +
                            "true so that performResize() sizes the render object."
                )
            }
            true
        }
    }

    // TODO(Migration/xbhatnag): Needs HitTestResult
//    /// Determines the set of render objects located at the given position.
//    ///
//    /// Returns true, and adds any render objects that contain the point to the
//    /// given hit test result, if this render object or one of its descendants
//    /// absorbs the hit (preventing objects below this one from being hit).
//    /// Returns false if the hit can continue to other objects below this one.
//    ///
//    /// The caller is responsible for transforming [position] from global
//    /// coordinates to its location relative to the origin of this [RenderBox].
//    /// This [RenderBox] is responsible for checking whether the given position is
//    /// within its bounds.
//    ///
//    /// Hit testing requires layout to be up-to-date but does not require painting
//    /// to be up-to-date. That means a render object can rely upon [performLayout]
//    /// having been called in [hitTest] but cannot rely upon [paint] having been
//    /// called. For example, a render object might be a child of a [RenderOpacity]
//    /// object, which calls [hitTest] on its children when its opacity is zero
//    /// even through it does not [paint] its children.
//    bool hitTest(HitTestResult result, { @required Offset position }) {
//        assert(() {
//            if (!hasSize) {
//                if (debugNeedsLayout) {
//                    throw new FlutterError(
//                            'Cannot hit test a render box that has never been laid out.\n'
//                    'The hitTest() method was called on this RenderBox:\n'
//                    '  $this\n'
//                    'Unfortunately, this object\'s geometry is not known at this time, '
//                    'probably because it has never been laid out. '
//                    'This means it cannot be accurately hit-tested. If you are trying '
//                    'to perform a hit test during the layout phase itself, make sure '
//                    'you only hit test nodes that have completed layout (e.g. the node\'s '
//                    'children, after their layout() method has been called).'
//                    );
//                }
//                throw new FlutterError(
//                        'Cannot hit test a render box with no size.\n'
//                'The hitTest() method was called on this RenderBox:\n'
//                '  $this\n'
//                'Although this node is not marked as needing layout, '
//                'its size is not set. A RenderBox object must have an '
//                'explicit size before it can be hit-tested. Make sure '
//                'that the RenderBox in question sets its size during layout.'
//                );
//            }
//            return true;
//        }());
//        if (_size.contains(position)) {
//            if (hitTestChildren(result, position: position) || hitTestSelf(position)) {
//                result.add(new BoxHitTestEntry(this, position));
//                return true;
//            }
//        }
//        return false;
//    }
//
    /**
     * Override this method if this render object can be hit even if its
     * children were not hit.
     *
     * The caller is responsible for transforming [position] from global
     * coordinates to its location relative to the origin of this [RenderBox].
     * This [RenderBox] is responsible for checking whether the given position is
     * within its bounds.
     *
     * Used by [hitTest]. If you override [hitTest] and do not call this
     * function, then you don't need to implement this function.
     */
    protected open fun hitTestSelf(position: Offset): Boolean = false

    // TODO(Migration/xbhatnag): Needs HitTestResult
//    /// Override this method to check whether any children are located at the
//    /// given position.
//    ///
//    /// Typically children should be hit-tested in reverse paint order so that
//    /// hit tests at locations where children overlap hit the child that is
//    /// visually "on top" (i.e., paints later).
//    ///
//    /// The caller is responsible for transforming [position] from global
//    /// coordinates to its location relative to the origin of this [RenderBox].
//    /// This [RenderBox] is responsible for checking whether the given position is
//    /// within its bounds.
//    ///
//    /// Used by [hitTest]. If you override [hitTest] and do not call this
//    /// function, then you don't need to implement this function.
//    @protected
//    bool hitTestChildren(HitTestResult result, { Offset position }) => false;

    // TODO(Migration/xbhatnag): Needs Matrix4
//    /// Multiply the transform from the parent's coordinate system to this box's
//    /// coordinate system into the given transform.
//    ///
//    /// This function is used to convert coordinate systems between boxes.
//    /// Subclasses that apply transforms during painting should override this
//    /// function to factor those transforms into the calculation.
//    ///
//    /// The [RenderBox] implementation takes care of adjusting the matrix for the
//    /// position of the given child as determined during layout and stored on the
//    /// child's [parentData] in the [BoxParentData.offset] field.
//    @override
//    void applyPaintTransform(RenderObject child, Matrix4 transform) {
//        assert(child != null);
//        assert(child.parent == this);
//        assert(() {
//            if (child.parentData is! BoxParentData) {
//            throw new FlutterError(
//                    '$runtimeType does not implement applyPaintTransform.\n'
//            'The following $runtimeType object:\n'
//            '  ${toStringShallow()}\n'
//            '...did not use a BoxParentData class for the parentData field of the following child:\n'
//            '  ${child.toStringShallow()}\n'
//            'The $runtimeType class inherits from RenderBox. '
//            'The default applyPaintTransform implementation provided by RenderBox assumes that the '
//            'children all use BoxParentData objects for their parentData field. '
//            'Since $runtimeType does not in fact use that ParentData class for its children, it must '
//            'provide an implementation of applyPaintTransform that supports the specific ParentData '
//            'subclass used by its children (which apparently is ${child.parentData.runtimeType}).'
//            );
//        }
//            return true;
//        }());
//        final BoxParentData childParentData = child.parentData;
//        final Offset offset = childParentData.offset;
//        transform.translate(offset.dx, offset.dy);
//    }

    // TODO(Migration/xbhatnag): Needs Matrix4
//    /// Convert the given point from the global coordinate system in logical pixels
//    /// to the local coordinate system for this box.
//    ///
//    /// If the transform from global coordinates to local coordinates is
//    /// degenerate, this function returns [Offset.zero].
//    ///
//    /// If `ancestor` is non-null, this function converts the given point from the
//    /// coordinate system of `ancestor` (which must be an ancestor of this render
//    /// object) instead of from the global coordinate system.
//    ///
//    /// This method is implemented in terms of [getTransformTo].
//    Offset globalToLocal(Offset point, { RenderObject ancestor }) {
//        final Matrix4 transform = getTransformTo(ancestor);
//        final double det = transform.invert();
//        if (det == 0.0)
//            return Offset.zero;
//        return MatrixUtils.transformPoint(transform, point);
//    }

    // TODO(Migration/xbhatnag): Needs MatrixUtils
//    /// Convert the given point from the local coordinate system for this box to
//    /// the global coordinate system in logical pixels.
//    ///
//    /// If `ancestor` is non-null, this function converts the given point to the
//    /// coordinate system of `ancestor` (which must be an ancestor of this render
//    /// object) instead of to the global coordinate system.
//    ///
//    /// This method is implemented in terms of [getTransformTo].
//    Offset localToGlobal(Offset point, { RenderObject ancestor }) {
//        return MatrixUtils.transformPoint(getTransformTo(ancestor), point);
//    }

    /**
     * Returns a rectangle that contains all the pixels painted by this box.
     *
     * The paint bounds can be larger or smaller than [size], which is the amount
     * of space this box takes up during layout. For example, if this box casts a
     * shadow, that shadow might extend beyond the space allocated to this box
     * during layout.
     *
     * The paint bounds are used to size the buffers into which this box paints.
     * If the box attempts to paints outside its paint bounds, there might not be
     * enough memory allocated to represent the box's visual appearance, which
     * can lead to undefined behavior.
     *
     * The returned paint bounds are in the local coordinate system of this box.
     */
    override val paintBounds: Rect
        get() = Offset.zero.and(size!!)

    // TODO(Migration/xbhatnag): RenderObject does not implement HitTestTarget
//    /// Override this method to handle pointer events that hit this render object.
//    ///
//    /// For [RenderBox] objects, the `entry` argument is a [BoxHitTestEntry]. From this
//    /// object you can determine the [PointerDownEvent]'s position in local coordinates.
//    /// (This is useful because [PointerEvent.position] is in global coordinates.)
//    ///
//    /// If you override this, consider calling [debugHandleEvent] as follows, so
//    /// that you can support [debugPaintPointersEnabled]:
//    ///
//    /// ```dart
//    /// @override
//    /// void handleEvent(PointerEvent event, HitTestEntry entry) {
//    ///   assert(debugHandleEvent(event, entry));
//    ///   // ... handle the event ...
//    /// }
//    /// ```
//    // TODO(ianh): Fix the type of the argument here once https://github.com/dart-lang/sdk/issues/25232 is fixed
//    @override
//    void handleEvent(PointerEvent event, covariant HitTestEntry entry) {
//        super.handleEvent(event, entry);
//    }

    var _debugActivePointers = 0

    // TODO(Migration/xbhatnag): Needs PointerEvent
//    /// Implements the [debugPaintPointersEnabled] debugging feature.
//    ///
//    /// [RenderBox] subclasses that implement [handleEvent] should call
//    /// [debugHandleEvent] from their [handleEvent] method, as follows:
//    ///
//    /// ```dart
//    /// @override
//    /// void handleEvent(PointerEvent event, HitTestEntry entry) {
//    ///   assert(debugHandleEvent(event, entry));
//    ///   // ... handle the event ...
//    /// }
//    /// ```
//    ///
//    /// If you call this for a [PointerDownEvent], make sure you also call it for
//    /// the corresponding [PointerUpEvent] or [PointerCancelEvent].
//    bool debugHandleEvent(PointerEvent event, HitTestEntry entry) {
//        assert(() {
//            if (debugPaintPointersEnabled) {
//                if (event is PointerDownEvent) {
//                    _debugActivePointers += 1;
//                } else if (event is PointerUpEvent || event is PointerCancelEvent) {
//                    _debugActivePointers -= 1;
//                }
//                markNeedsPaint();
//            }
//            return true;
//        }());
//        return true;
//    }

    override fun debugPaint(context: PaintingContext, offset: Offset) {
        assert {
            if (debugPaintSizeEnabled)
                debugPaintSize(context, offset)
            if (debugPaintBaselinesEnabled)
                debugPaintBaselines(context, offset)
            if (debugPaintPointersEnabled)
                debugPaintPointers(context, offset)
            true
        }
    }

    /**
     * In debug mode, paints a border around this render box.
     *
     * Called for every [RenderBox] when [debugPaintSizeEnabled] is true.
     */
    protected open fun debugPaintSize(context: PaintingContext, offset: Offset) {
        assert {
            val paint = Paint().apply {
                style = PaintingStyle.stroke
                strokeWidth = 1.0
                color = Color(0xFF00FFFF.toInt())
            }
            context.canvas.drawRect((offset.and(size!!)).deflate(0.5), paint)
            true
        }
    }

    /**
     * In debug mode, paints a line for each baseline.
     *
     * Called for every [RenderBox] when [debugPaintBaselinesEnabled] is true.
     */
    protected fun debugPaintBaselines(context: PaintingContext, offset: Offset) {
        // TODO(Migration/andrey): Needs TextBaseline
        TODO()
//        assert {
//            val paint = Paint().apply {
//                style = PaintingStyle.stroke
//                strokeWidth = 0.25
//            }
//            var path : Path? = null
//            // ideographic baseline
//            val baselineI : Double? = getDistanceToBaseline(TextBaseline.ideographic, onlyReal: true);
//            if (baselineI != null) {
//                paint.color = 0xFFFFD000.toInt()
//                path = Path()
//                path.moveTo(offset.dx, offset.dy + baselineI)
//                path.lineTo(offset.dx + size!!.width, offset.dy + baselineI)
//                context.canvas.drawPath(path, paint);
//            }
//            // alphabetic baseline
//            val baselineA : Double? = getDistanceToBaseline(TextBaseline.alphabetic, onlyReal: true);
//            if (baselineA != null) {
//                paint.color = 0xFF00FF00.toInt()
//                path = Path();
//                path.moveTo(offset.dx, offset.dy + baselineA);
//                path.lineTo(offset.dx + size!!.width, offset.dy + baselineA);
//                context.canvas.drawPath(path, paint);
//            }
//            true
//        }
    }

    /**
     * In debug mode, paints a rectangle if this render box has counted more
     * pointer downs than pointer up events.
     *
     * Called for every [RenderBox] when [debugPaintPointersEnabled] is true.
     *
     * By default, events are not counted. For details on how to ensure that
     * events are counted for your class, see [debugHandleEvent].
     */
    protected fun debugPaintPointers(context: PaintingContext, offset: Offset) {
        assert {
            if (_debugActivePointers > 0) {
                val paint = Paint().apply {
                    // new Color(0x00BBBB | ((0x04000000 * depth) & 0xFF000000));
                    color = Color(0x00BBBB or ((0x04000000 * depth) and 0xFF000000.toInt()))
                }
                context.canvas.drawRect(offset.and(size!!), paint)
            }
            true
        }
    }

    override fun debugFillProperties(properties: DiagnosticPropertiesBuilder) {
        super.debugFillProperties(properties)
        properties.add(DiagnosticsProperty.create("size", _size, missingIfNull = true))
    }
}