package com.bitwig.extensions.controllers.devine;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.ControllerExtension;

public class EzCreatorKeyXL extends EzCreatorKeyCommonExtension
{
   public EzCreatorKeyXL(final EzCreatorKeyXLDefinition definition, final ControllerHost host)
   {
      super(definition, host, 0x02);
   }
}
