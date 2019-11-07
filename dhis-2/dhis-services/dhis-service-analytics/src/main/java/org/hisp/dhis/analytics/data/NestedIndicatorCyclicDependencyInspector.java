/*
 * Copyright (c) 2004-2019, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.hisp.dhis.analytics.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.hisp.dhis.common.CyclicReferenceException;
import org.hisp.dhis.common.DimensionalItemId;
import org.hisp.dhis.expression.ExpressionService;
import org.hisp.dhis.expression.ParseType;
import org.hisp.dhis.indicator.Indicator;

import com.scalified.tree.TreeNode;
import com.scalified.tree.multinode.ArrayMultiTreeNode;

import static org.hisp.dhis.common.DimensionItemType.INDICATOR;

/**
 * Builds a tree structure representing nested Indicators and detects if an Indicator has a cyclic dependency
 * within the same tree.
 *
 *
 * @author Luciano Fiandesio
 */
public class NestedIndicatorCyclicDependencyInspector
{
    /**
     * Holds the tree structure of the nested indicators
     */
    private List<TreeNode<String>> nestedIndicatorTreesNum;

    private List<TreeNode<String>> nestedIndicatorTreesDen;

    private ExpressionService expressionService;

    /**
     * Initialize the component
     *
     * @param indicators a List of Indicators representing the root of the trees. Each Indicator passed in the
     *                   constructor will create a new tree structure
     *
     * @param expressionService the {@see ExpressionService} required to resolve expressions
     */
    public NestedIndicatorCyclicDependencyInspector( List<Indicator> indicators, ExpressionService expressionService )
    {
        this.expressionService = expressionService;
        // init the tree structures
        resetTrees();

        // add root nodes represented by the Indicators
        for ( Indicator indicator : indicators )
        {
            nestedIndicatorTreesNum.add( getNode( indicator, ExpressionType.NUMERATOR ) );
            nestedIndicatorTreesDen.add( getNode( indicator, ExpressionType.DENOMINATOR ) );
        }
    }

    /**
     * Add the List of {@see Indicator} to the the Tree
     * 
     * Throws a {@see CyclicReferenceException} if the indicators are already in the
     * Tree
     * 
     * @param indicators
     */
    public void add( List<Indicator> indicators )
    {
        add( indicators, ExpressionType.NUMERATOR );
        add( indicators, ExpressionType.DENOMINATOR );
    }

    private void add( List<Indicator> indicators, ExpressionType expressionType )
    {
        List<String> alreadyAdded = new ArrayList<>();

        for ( Indicator indicator : indicators )
        {
            if ( !alreadyAdded.contains( indicator.getUid() ) )
            {
                for ( TreeNode<String> node : (expressionType.equals( ExpressionType.DENOMINATOR )
                    ? this.nestedIndicatorTreesDen
                    : this.nestedIndicatorTreesNum) )
                {
                    TreeNode<String> root = node.root();
                    TreeNode<String> nodeToReplace = root.find( indicator.getUid() );

                    if ( nodeToReplace != null && !nodeToReplace.isRoot() )
                    {
                        if ( nodeToReplace.isLeaf() )
                        {
                            // Replace the "write-ahead" node with the "real" node
                            TreeNode<String> parent = nodeToReplace.parent();
                            root.remove( nodeToReplace );
                            parent.add( getNode( indicator, expressionType ) );
                            alreadyAdded.add( indicator.getUid() );
                            break;
                        }
                        // If the node is not leaf, it means that it was found
                        // up in the tree structure, therefore we need to
                        // throw an exception
                        else
                        {
                            throw new CyclicReferenceException( String.format(
                                "An Indicator with identifier '%s' has a cyclic reference to another item in the %s expression.",
                                nodeToReplace.data(),
                                expressionType.equals( ExpressionType.DENOMINATOR ) ? "Denominator" : "Numerator" ) );
                        }
                    }
                }
            }
        }
    }

    private TreeNode<String> getNode( Indicator indicator, ExpressionType expressionType )
    {
        // Create the Node using the Indicator UID as value
        TreeNode<String> node = new ArrayMultiTreeNode<>( indicator.getUid() );

        // Add to the newly created node all the DimensionalItems found in the numerator
        // and denominator expressions as
        // child nodes ("Write-ahead"). The write-ahead nodes are required to "connect"
        // the next iteration of Indicators
        //
        Set<DimensionalItemId> expressionDataElements = expressionService.getExpressionDimensionalItemIds(
            expressionType.equals( ExpressionType.DENOMINATOR ) ? indicator.getDenominator() : indicator.getNumerator(),
            ParseType.INDICATOR_EXPRESSION );

        for ( DimensionalItemId dimensionalItemId : expressionDataElements )
        {
            // Only add Indicators to the tree, since Indicators can be nested
            if ( dimensionalItemId.getDimensionItemType().equals( INDICATOR ) )
            {
                node.add( new ArrayMultiTreeNode<>( dimensionalItemId.getId0() ) );
            }
        }

        return node;
    }


    private void resetTrees()
    {
        nestedIndicatorTreesNum = new ArrayList<>();
        nestedIndicatorTreesDen = new ArrayList<>();
    }

    enum ExpressionType
    {
        NUMERATOR, DENOMINATOR
    }
}
