package mrtjp.projectred.transportation

import codechicken.lib.data.MCDataInput
import codechicken.lib.gui.GuiDraw
import codechicken.lib.packet.PacketCustom
import codechicken.lib.render.FontUtils
import codechicken.lib.vec.BlockCoord
import cpw.mods.fml.relauncher.{Side, SideOnly}
import mrtjp.core.color.{Colors, Colors_old}
import mrtjp.core.gui._
import mrtjp.core.item.{ItemKey, ItemKeyStack}
import mrtjp.core.resource.ResourceLib
import mrtjp.core.vec.{Point, Rect, Size, Vec2}
import mrtjp.projectred.core.libmc._
import net.minecraft.client.gui.Gui
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.Container
import net.minecraft.util.EnumChatFormatting
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11

class GuiCraftingPipe(container:Container, pipe:RoutedCraftingPipePart) extends NodeGui(container, 176, 220)
{
    override def onAddedToParent_Impl()
    {
        def sendClick(message:String)
        {
            val packet = new PacketCustom(TransportationCPH.channel, TransportationCPH.gui_CraftingPipe_action)
            packet.writeCoord(new BlockCoord(pipe.tile))
            packet.writeString(message)
            packet.sendToServer()
        }

        val up = new MCButtonNode
        up.position = Point(138, 12)
        up.size = Size(20, 14)
        up.text = "up"
        up.clickDelegate = {() => sendClick("up")}
        addChild(up)

        val down = new MCButtonNode
        down.position = Point(92, 12)
        down.size = Size(20, 14)
        down.text = "down"
        down.clickDelegate = {() => sendClick("down")}
        addChild(down)
    }

    override def drawBack_Impl(mouse:Point, frame:Float)
    {
        PRResources.guiPipeCrafting.bind()
        drawTexturedModalRect(0, 0, 0, 0, xSize, ySize)
        FontUtils.drawCenteredString(""+pipe.priority, 126, 15, Colors_old.BLACK.rgb)
        GuiLib.drawPlayerInvBackground(8, 138)

        var color = 0
        ResourceLib.guiExtras.bind()

        for ((x, y) <- GuiLib.createSlotGrid(8, 108, 9, 1, 0, 0))
        {
            GL11.glColor4f(1, 1, 1, 1)
            drawTexturedModalRect(x, y, 1, 11, 16, 16)
            Gui.drawRect(x+4, y-2, x+4+8, y, Colors_old.get(color).argb)
            color += 1
        }
    }

    override def drawFront_Impl(mouse:Point, frame:Float)
    {
        PRResources.guiPipeCrafting.bind()
        val oldZ = zLevel
        zLevel = 300
        var i = 0
        for ((x, y) <- GuiLib.createSlotGrid(20, 12, 2, 4, 20, 0))
        {
            val u = 178
            val v = if (inventorySlots.getSlot(i).getStack == null) 107 else 85
            i += 1
            drawTexturedModalRect(x-5, y-2, u, v, 25, 20)
        }
        zLevel = oldZ
    }
}

object GuiCraftingPipe extends TGuiBuilder
{
    override def getID = TransportationProxy.guiIDCraftingPipe

    @SideOnly(Side.CLIENT)
    override def buildGui(player:EntityPlayer, data:MCDataInput) =
    {
        PRLib.getMultiPart(player.worldObj, data.readCoord(), 6) match
        {
            case pipe:RoutedCraftingPipePart =>
                new GuiCraftingPipe(pipe.createContainer(player), pipe)
            case _ => null
        }
    }
}

class GuiExtensionPipe(container:Container, id:String) extends NodeGui(container)
{
    override def drawBack_Impl(mouse:Point, frame:Float)
    {
        GuiLib.drawGuiBox(0, 0, size.width, size.height, zLevel)
        GuiLib.drawPlayerInvBackground(8, 84)

        fontRenderer.drawString("Extension ID:", 10, 10, 0xff000000)

        var i = 0
        for (s <- id.split("-"))
        {
            fontRenderer.drawString(s, 10, 25+10*i, 0xff000000)
            i+=1
        }

        GuiLib.drawSlotBackground(133, 19)
        GuiLib.drawSlotBackground(133, 49)
        ResourceLib.guiExtras.bind()
        drawTexturedModalRect(134, 20, 1, 11, 16, 16)
    }
}

object GuiExtensionPipe extends TGuiBuilder
{
    override def getID = TransportationProxy.guiIDExtensionPipe

    @SideOnly(Side.CLIENT)
    override def buildGui(player:EntityPlayer, data:MCDataInput) =
    {
        val coord = data.readCoord()
        val id = data.readString()
        PRLib.getMultiPart(player.worldObj, coord, 6) match
        {
            case pipe:RoutedExtensionPipePart =>
                new GuiExtensionPipe(pipe.createContainer(player), id)
            case _ => null
        }
    }
}

class GuiInterfacePipe(container:Container, pipe:RoutedInterfacePipePart) extends NodeGui(container, 176, 200)
{
    override def drawBack_Impl(mouse:Point, frame:Float)
    {
        PRResources.guiPipeInterface.bind()
        drawTexturedModalRect(0, 0, 0, 0, xSize, ySize)
        GuiLib.drawPlayerInvBackground(8, 118)
    }

    override def drawFront_Impl(mouse:Point, frame:Float)
    {
        PRResources.guiPipeInterface.bind()
        val oldZ = zLevel
        zLevel = 300

        for (i <- 0 until 4)
        {
            val x = 19
            val y = 10+i*26
            val u = 178
            val v = if (inventorySlots.getSlot(i).getStack == null) 107 else 85
            drawTexturedModalRect(x, y, u, v, 25, 20)
        }
        zLevel = oldZ
    }
}

object GuiInterfacePipe extends TGuiBuilder
{
    override def getID = TransportationProxy.guiIDInterfacePipe

    @SideOnly(Side.CLIENT)
    override def buildGui(player:EntityPlayer, data:MCDataInput) =
    {
        val coord = data.readCoord()
        PRLib.getMultiPart(player.worldObj, coord, 6) match
        {
            case pipe:RoutedInterfacePipePart =>
                new GuiInterfacePipe(pipe.createContainer(player), pipe)
            case _ => null
        }
    }
}

class GuiRequester(pipe:IWorldRequester) extends NodeGui(256, 192)
{
    var clip:ClipNode = null
    var pan:PanNode = null
    var list:ItemListNode = null
    var selectedItem:ItemKey = null

    var itemMap = Map.empty[ItemKey, Int]

    var textFilter:SimpleTextboxNode = null
    var textCount:SimpleTextboxNode = null

    var pull:CheckBoxNode = null
    var craft:CheckBoxNode = null
    var partials:CheckBoxNode = null

    {
        clip = new ClipNode
        clip.position = Point(18, 18)
        clip.size = Size(220, 117)
        addChild(clip)

        pan = new PanNode
        pan.size = Size(220, 117)
        pan.scrollBarThickness = 16
        pan.scrollModifier = Vec2(0, 1)
        pan.scrollBarHorizontal = false
        pan.panDelegate = {() => refreshList()}
        clip.addChild(pan)

        list = new ItemListNode
        list.zPosition = -0.01
        list.itemSize = Size(16, 16)
        list.gridWidth = 12
        list.displayNodeFactory = {stack =>
            val d = new ItemDisplayNode
            d.zPosition = -0.01
            d.backgroundColour = if (stack.key == selectedItem)
                Colors.LIME.argb(0x44) else 0
            d.clickDelegate = {() =>
                selectedItem = stack.key
                refreshList()
            }
            d
        }
        pan.addChild(list)

        textFilter = new SimpleTextboxNode
        textFilter.position = Point(54, 139)
        textFilter.size = Size(148, 16)
        textFilter.phantom = "search"
        textFilter.textChangedDelegate = {() => refreshList()}
        addChild(textFilter)

        textCount = new SimpleTextboxNode
        {
            override def mouseScrolled_Impl(p:Point, dir:Int, consumed:Boolean) =
            {
                if (!consumed && rayTest(p))
                {
                    if (dir > 0) countUp()
                    else if (dir < 0) countDown()
                    true
                }
                else false
            }
        }
        textCount.position = Point(102, 158)
        textCount.size = Size(50, 16)
        textCount.text = "1"
        textCount.phantom = "1"
        textCount.allowedcharacters = "0123456789"
        textCount.focusChangeDelegate = {() =>
            if (!textCount.focused)
                if (textCount.text.isEmpty || Integer.parseInt(textCount.text) < 1)
                    textCount.text = "1"
        }
        addChild(textCount)

        pull = CheckBoxNode.centered(210, 148)
        pull.state = true
        pull.clickDelegate = {() => askForListRefresh()}
        addChild(pull)

        craft = CheckBoxNode.centered(210, 163)
        craft.state = true
        craft.clickDelegate = {() => askForListRefresh()}
        addChild(craft)

        partials = CheckBoxNode.centered(210, 178)
        addChild(partials)

        val ref = new MCButtonNode
        ref.position = Point(10, 158)
        ref.size = Size(50, 14)
        ref.text = "Refresh"
        ref.clickDelegate = {() => askForListRefresh()}
        addChild(ref)

        val req = new MCButtonNode
        req.position = Point(10, 173)
        req.size = Size(50, 14)
        req.text = "Submit"
        req.clickDelegate = {() => sendItemRequest()}
        addChild(req)

        val down = new MCButtonNode
        down.position = Point(81, 158)
        down.size = Size(16, 16)
        down.text = "-"
        down.clickDelegate = {() => countDown()}
        addChild(down)

        val up = new MCButtonNode
        up.position = Point(156, 158)
        up.size = Size(16, 16)
        up.text = "+"
        up.clickDelegate = {() => countUp()}
        addChild(up)

        val all = new MCButtonNode
        all.position = Point(176, 158)
        all.size = Size(24, 16)
        all.text = "All"
        all.clickDelegate = {() => if (selectedItem != null) textCount.text = String.valueOf(Math.max(1, itemMap(selectedItem)))}
        addChild(all)
    }

    def refreshList()
    {
        list.items = itemMap.map(p => ItemKeyStack.get(p._1, p._2)).toSeq.filter(filterAllows).sorted
        list.reset()

        if (!list.items.exists(_.key == selectedItem))
            selectedItem = null

        def filterAllows(stack:ItemKeyStack):Boolean =
        {
            def stringMatch(name:String, filter:String):Boolean =
            {
                for (s <- filter.split(" ")) if (!name.contains(s)) return false
                true
            }

            if (stringMatch(stack.key.getName.toLowerCase, textFilter.text)) true
            else false
        }
    }

    override def drawBack_Impl(mouse:Point, frame:Float)
    {
        PRResources.guiPipeRequest.bind()
        GuiDraw.drawTexturedModalRect(0, 0, 0, 0, size.width, size.height)
    }

    override def drawFront_Impl(mouse:Point, frame:Float)
    {
        GuiDraw.drawString("Pull", 218, 144, Colors.GREY.rgb, false)
        GuiDraw.drawString("Craft", 218, 159, Colors.GREY.rgb, false)
        GuiDraw.drawString("Parials", 218, 174, Colors.GREY.rgb, false)
    }

    override def onAddedToParent_Impl()
    {
        askForListRefresh()
        list.cullFrame = convertRectToScreen(Rect(Point(18, 18), Size(220, 117)))
    }

    private def sendItemRequest()
    {
        val count = textCount.text
        if (count.isEmpty) return

        val amount = Integer.parseInt(count)
        if (amount <= 0) return

        val request = selectedItem
        if (request != null)
        {
            val packet = new PacketCustom(TransportationSPH.channel, TransportationSPH.gui_Request_submit)
            packet.writeCoord(new BlockCoord(pipe.getContainer.tile))
            packet.writeBoolean(pull.state)
            packet.writeBoolean(craft.state)
            packet.writeBoolean(partials.state)
            packet.writeItemStack(request.makeStack(amount), true)
            packet.sendToServer()
        }
    }

    private def askForListRefresh()
    {
        val packet = new PacketCustom(TransportationSPH.channel, TransportationSPH.gui_Request_listRefresh)
        packet.writeCoord(new BlockCoord(pipe.getContainer.tile))
        packet.writeBoolean(pull.state)
        packet.writeBoolean(craft.state)
        packet.sendToServer()
    }

    private def countUp()
    {
        var current = 0
        val s = textCount.text
        if (s != null && !s.isEmpty) current = Integer.parseInt(s)

        val newCount =
            if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) current+10
            else current+1

        if (newCount < 999999999) textCount.text = ""+newCount
    }

    private def countDown()
    {
        val s = textCount.text
        val current = if (s.nonEmpty) Integer.parseInt(s) else 1

        val newCount =
            (if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) current-10
            else current-1) max 1

        textCount.text = ""+newCount
    }

    def receiveContentList(content:Map[ItemKey, Int])
    {
        itemMap = content
        refreshList()
    }

    override def keyPressed_Impl(c:Char, keycode:Int, consumed:Boolean) =
    {
        if (!consumed && keycode == Keyboard.KEY_RETURN)
        {
            textFilter.setFocused(true)
            if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT))
                textFilter.setText("")
            true
        }
        else false
    }

    override def mouseScrolled_Impl(p:Point, dir:Int, consumed:Boolean) =
    {
        if (!consumed && clip.frame.contains(convertPointFromScreen(p)))
        {
            if (dir > 0) pan.panChildren(Vec2.down*3)
            else if (dir < 0) pan.panChildren(Vec2.up*3)
            true
        }
        else false
    }
}

class GuiFirewallPipe(pipe:RoutedFirewallPipe, c:Container) extends NodeGui(c, 176, 184)
{
    {
        val excl = new IconButtonNode
        {
            override def drawButton(mouseover:Boolean)
            {
                ResourceLib.guiExtras.bind()
                GuiDraw.drawTexturedModalRect(position.x, position.y, if (pipe.filtExclude) 1 else 17, 102, 14, 14)
            }
        }
        excl.position = Point(113, 45)
        excl.size = Size(14, 14)
        excl.tooltipBuilder = {_ += ("Items are "+
                (if (pipe.filtExclude) "blacklisted" else "whitelisted"))}
        excl.clickDelegate = {() => sendMessage(0)}
        addChild(excl)

        def makeButton(x:Int, y:Int, f: => Boolean, desc:String, id:Int)
        {
            val b = new IconButtonNode
            {
                override def drawButton(mouseover:Boolean)
                {
                    ResourceLib.guiExtras.bind()
                    GuiDraw.drawTexturedModalRect(x, y, if (f) 33 else 49, 134, 14, 14)
                }
            }
            b.position = Point(x, y)
            b.size = Size(14, 14)
            b.tooltipBuilder = {_ += desc}
            b.clickDelegate = {() => sendMessage(id)}
            addChild(b)
        }

        makeButton(150, 28, pipe.allowRoute, "Push routing", 1)
        makeButton(150, 45, pipe.allowBroadcast, "Pulling", 2)
        makeButton(150, 62, pipe.allowCrafting, "Crafting", 3)
    }

    def sendMessage(id:Int)
    {
        new PacketCustom(TransportationCPH.channel, TransportationCPH.gui_FirewallPipe_action)
            .writeCoord(new BlockCoord(pipe.tile)).writeByte(id).sendToServer()
    }

    override def drawBack_Impl(mouse:Point, frame:Float)
    {
        PRResources.guiPipeFirewall.bind()
        GuiDraw.drawTexturedModalRect(0, 0, 0, 0, size.width, size.height)
        GuiDraw.drawString("Firewall Pipe", 8, 6, Colors.GREY.argb, false)
    }
}

object GuiFirewallPipe extends TGuiBuilder
{
    override def getID = TransportationProxy.guiIDFirewallPipe

    @SideOnly(Side.CLIENT)
    override def buildGui(player:EntityPlayer, data:MCDataInput) =
    {
        PRLib.getMultiPart(player.worldObj, data.readCoord(), 6) match
        {
            case pipe:RoutedFirewallPipe =>
                pipe.filtExclude = data.readBoolean()
                pipe.allowRoute = data.readBoolean()
                pipe.allowBroadcast = data.readBoolean()
                pipe.allowCrafting = data.readBoolean()
                new GuiFirewallPipe(pipe, pipe.createContainer(player))
            case _ =>
                for (i <- 0 until 4) data.readBoolean()
                null
        }
    }
}