package appeng.bootstrap.components;


import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.relauncher.Side;

import appeng.tile.AEBaseTile;


/**
 * @author Sebastian
 * @version rv3 - 20.08.2016
 * @since rv3 20.08.2016
 */
public class TesrComponent<T extends AEBaseTile> implements PreInitComponent
{

	private final Class<T> tileEntityClass;

	private final TileEntitySpecialRenderer<? super T> tesr;

	public TesrComponent( Class<T> tileEntityClass, TileEntitySpecialRenderer<? super T> tesr )
	{
		this.tileEntityClass = tileEntityClass;
		this.tesr = tesr;
	}

	@Override
	public void preInitialize( Side side )
	{
		ClientRegistry.bindTileEntitySpecialRenderer( tileEntityClass, tesr );
	}
}
